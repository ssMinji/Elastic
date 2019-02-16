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
import org.sweble.wikitext.engine.output.HtmlRenderer;
import org.sweble.wikitext.engine.output.HtmlRendererCallback;
import org.sweble.wikitext.engine.output.MediaInfo;
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp;
import org.sweble.wikitext.engine.utils.UrlEncoding;
import org.sweble.wikitext.parser.nodes.WtUrl;
import org.sweble.wikitext.parser.parser.LinkTargetException;

public class SwebleApp {

	public static class TokenizerMapper extends Mapper<Object, Text, Text, Text> {

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String string = new String(value.toString());
			String xml = string.replaceAll("^.*<page>", "<page>") + "</page>";
			Document wikitext = Jsoup.parse(xml, "", Parser.xmlParser());
			wikitext.outputSettings(new Document.OutputSettings().prettyPrint(false));

			String title = wikitext.select("page>title").text();

			String text = "";

			try {
				for (TextNode node : wikitext.select("text").get(0).textNodes()) {
					text = text + node + "\n\n";
				}

				text = StringEscapeUtils.unescapeXml(text);

				String html = null;

				html = run(text, "plaintext", true);
				
				Document plaintext = Jsoup.parse(html, "", Parser.htmlParser());
				
				String descrip = plaintext.select("body").text();

				context.write(new Text(title), new Text(descrip));
				
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

			if (renderHtml) {
				String ourHtml = HtmlRenderer.print(new MyRendererCallback(), config, pageTitle, cp.getPage());
				String html = "<html><body>" + ourHtml + "</body>" + "</html>";

				return html;
			} else {
				TextConverter p = new TextConverter(config, wrapCol);
				return (String) p.go(cp.getPage());
			}

		}

		private static final class MyRendererCallback implements HtmlRendererCallback {
			protected static final String LOCAL_URL = "";

			@Override
			public boolean resourceExists(PageTitle target) {
				// TODO: Add proper check
				return false;
			}

			@Override
			public MediaInfo getMediaInfo(String title, int width, int height) {
				// TODO: Return proper media info
				return null;
			}

			@Override
			public String makeUrl(PageTitle target) {
				String page = UrlEncoding.WIKI.encode(target.getNormalizedFullTitle());
				String f = target.getFragment();
				String url = page;
				if (f != null && !f.isEmpty())
					url = page + "#" + UrlEncoding.WIKI.encode(f);
				return LOCAL_URL + "/" + url;
			}

			@Override
			public String makeUrl(WtUrl target) {
				if (target.getProtocol() == "")
					return target.getPath();
				return target.getProtocol() + ":" + target.getPath();
			}

			@Override
			public String makeUrlMissingTarget(String path) {
				return LOCAL_URL + "?title=" + path + "&amp;action=edit&amp;redlink=1";

			}
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
		job.setJarByClass(SwebleApp.class);
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