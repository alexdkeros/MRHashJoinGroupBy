package helperClasses;

import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Merger {

	/*
	 * Reducer
	 */
	public static class SimpleReducer extends Reducer<LongWritable, Text, NullWritable, Text> {

		@Override
		public void setup(Context context) throws IOException, InterruptedException{

			Configuration conf=context.getConfiguration();
			if (conf.get("first-line")!=null)
				context.write(NullWritable.get(),new Text(conf.get("first-line")));
		}
		
		
		@Override
		public void reduce(LongWritable key, Iterable<Text> values,Context context) 
				throws IOException, InterruptedException {
			
			for (Iterator<Text> iterator = values.iterator(); iterator.hasNext();) {
				Text value = (Text) iterator.next();
				context.write(NullWritable.get(), value);
			}
		}

	}
	
	public static boolean run(Path p0,Path outp, Configuration conf) throws IOException, ClassNotFoundException, InterruptedException{

		//--job configuration--
		Job job = new Job(conf,"Merge");
		job.setJarByClass(Merger.class);

		//num of reducers
		job.setNumReduceTasks(1);
		
		//inputs
		FileInputFormat.addInputPath(job, p0);
		//output
		FileOutputFormat.setOutputPath(job, outp);
		//classes
		job.setMapperClass(Mapper.class);
		job.setReducerClass(SimpleReducer.class);
		//map output
		job.setMapOutputKeyClass(LongWritable.class);
		job.setMapOutputValueClass(Text.class);
		//reducer output
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		
		return job.waitForCompletion(true);
		
	}
	
	public static void main(String[] args) throws Exception {
		//correct usage check
		if (args.length < 2 || args.length > 3) {
			System.err.println("Usage: Merger <input path (folder)> <output path> <first line(optional)>");
			System.exit(-1);
		}
		
		Path p0=new Path(args[0]);
		Path outp=new Path(args[1]);

		Configuration conf=new Configuration();
		
		if (args.length==3){
			conf.set("first-line", args[2]);
		}
		
		System.exit( run(p0,outp,conf)? 0:1);
	}
	
}
