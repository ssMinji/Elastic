package step01.datacleansing;

import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.sweble.wikitext.engine.EngineException;
import org.sweble.wikitext.engine.PageId;
import org.sweble.wikitext.engine.PageTitle;
import org.sweble.wikitext.engine.WtEngineImpl;
import org.sweble.wikitext.engine.config.WikiConfig;
import org.sweble.wikitext.engine.nodes.EngProcessedPage;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.parser.parser.LinkTargetException;

import com.google.gson.Gson;


public class SwebleParse {
	
	public static class WikiDTO {
		private String title;
		private String descrip;

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescrip() {
			return descrip;
		}

		public void setDescrip(String descrip) {
			this.descrip = descrip;
		}
	}

	public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String string = new String(value.toString());
			String xml = string.replaceAll("^.*<page>", "<page>") + "</page>";
			Document wikitext = Jsoup.parse(xml, "", Parser.xmlParser());
			wikitext.outputSettings(new Document.OutputSettings().prettyPrint(false));

			String title = wikitext.select("page>title").text();
			String id = wikitext.select("page>id").text();

			String text = "";

			try {
				for (TextNode node : wikitext.select("text").get(0).textNodes()) {
					text = text + node + "\n\n";
				}

				text = StringEscapeUtils.unescapeXml(text);

				String descrip = null;

				descrip = run(text, "plaintext", false);
				
				descrip = descrip.replaceAll("\\[\\[파일:.*\\]\\]", "");

				Gson gson = new Gson();
				WikiDTO d = new WikiDTO();
				d.setTitle(title);
				d.setDescrip(descrip);
				String json = gson.toJson(d);
				
				context.write(new Text(id), new Text(json));

			} catch (IndexOutOfBoundsException i) {
				i.printStackTrace();
			} catch (LinkTargetException e) {
				e.printStackTrace();
			} catch (EngineException e) {
				e.printStackTrace();
			}

		}

		static String run(String file, String fileTitle, boolean renderHtml)
				throws IOException, LinkTargetException, EngineException {

			WikiConfig config = DefaultConfigEnWp.generate();

			final int wrapCol = 80;

			WtEngineImpl engine = new WtEngineImpl(config);

			PageTitle pageTitle = PageTitle.make(config, fileTitle);

			PageId pageId = new PageId(pageTitle, -1);

			String wikitext = file;

			EngProcessedPage cp = engine.postprocess(pageId, wikitext, null);

			TextConverter p = new TextConverter(config, wrapCol);
			return (String) p.go(cp.getPage());

		}
	}

	public static class ListReducer extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Text values, Context context) throws IOException, InterruptedException {
			context.write(key, values);
		}

	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.set("textinputformat.record.delimiter", "</page>");
		Job job = Job.getInstance(conf, "xml sweble parsing");
		job.setJarByClass(SwebleParse.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setReducerClass(ListReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(5);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}