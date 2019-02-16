package step02.join;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class RedirectRmXml {
	public static class MetaMapper extends Mapper<Object, Text, Text, Text> {
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			String[] parts = StringUtils.splitPreserveAllTokens(value.toString(), "\t");
			String body = parts[1].replaceAll(" ", "_");
			String id = parts[0];
			context.write(new Text(id), new Text("body\t" + body));
		}
	}

	public static class ReMapper extends Mapper<Object, Text, Text, Text> {
			
			public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
				String[] parts = StringUtils.splitPreserveAllTokens(value.toString(), "\t");
				String title = parts[1];
				String id = parts[0];
				context.write(new Text(id), new Text("Redirect\t" + title));
			}
	}

	public static class ReduceJoinReducer extends Reducer<Text, Text, Text, Text> {
	
			public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
				String body = null;
				String rebody = null;
					for (Text value : values) {
						String parts[] = StringUtils.splitPreserveAllTokens(value.toString(), "\t");
						if (parts[0].equals("body")) {
							body = parts[1];
						} else if(parts[0].equals("Redirect")) {
							rebody = parts[1];
						}
					}	
					if (rebody == null) {
						context.write(key, new Text(body));
					}
			}
	}


	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = new Job(conf, "redirect remove from swebleparsing result");
		job.setJarByClass(RedirectRmXml.class);
		job.setReducerClass(ReduceJoinReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setNumReduceTasks(5);
		
		MultipleInputs.addInputPath(job, new Path(args[0]),TextInputFormat.class, MetaMapper.class);
		MultipleInputs.addInputPath(job, new Path(args[1]),TextInputFormat.class, ReMapper.class);
		Path outputPath = new Path(args[2]);
		
		
		FileOutputFormat.setOutputPath(job, outputPath);
		outputPath.getFileSystem(conf).delete(outputPath);
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}

}
