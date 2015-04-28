package groupBy;

import helperClasses.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.collect.Iterables;

public class MRGroupBy {
	
	/*
	 * Mapper
	 */
	public static class GroupByMapper extends Mapper<LongWritable,Text,Text,Text>{
		
		//custom parameters
		private String delim;
		private int[] columnPos;
		private String[] groupByColsArray;
		
		@Override
		public void setup(Context context){
			

			Configuration conf=context.getConfiguration();

			//get delimiter from configuration
			delim=conf.get("delimiter");
			
			//get group by positions
			String groupByCols=conf.get("group_by_cols");
			groupByColsArray=groupByCols.split(delim);
			columnPos=new int[groupByColsArray.length];
			for (int i=0; i<groupByColsArray.length;i++){
				columnPos[i]=new Integer(groupByColsArray[i]);
			}
			groupByColsArray=new String[columnPos.length];			
		}
		
		@Override
		public void map(LongWritable key, Text value, Context context) 
				throws IOException, InterruptedException {
						
			if (!(key.equals(new LongWritable(0)) && context.getConfiguration().getBoolean("ignoreZeroLine", false))){ //ignore first line of file containing column names

				String[] values=(value.toString()).split(delim);

				for (int i=0;i<columnPos.length;i++){
					groupByColsArray[i]=values[columnPos[i]];
				}
				
				context.write(new Text(StringUtils.join(groupByColsArray,delim)),value);
			}
		}

	}
	
	/*
	 * Reducer
	 */
	public static class GroupByReducer extends Reducer<Text, Text, NullWritable, Text> {

		private String delim;
		
		@Override
		public void setup(Context context){

			Configuration conf=context.getConfiguration();

			//get delimiter from configuration
			delim=conf.get("delimiter");
						
		}
		
		@Override
		public void reduce(Text key, Iterable<Text> values,Context context) 
				throws IOException, InterruptedException {
			
			
			context.write(NullWritable.get(),new Text(key+delim+Iterables.size(values)));
		}
	}
	
	public static boolean run(Path p0,Path outp, Configuration conf,String groupBy,String cols,int reducerNum) throws IOException, ClassNotFoundException, InterruptedException{

		//filesystem
		FileSystem hdfs=FileSystem.get(conf);
		System.out.println(hdfs.getWorkingDirectory()); //DBG
		

		System.out.println("P0: "+hdfs.exists(p0));	//DBG
		System.out.println("OutP: "+hdfs.exists(outp));	//DBG
		
		String colsRel=cols;
		if (colsRel==null){
			//--relation--
			//get column names from relation
			BufferedReader br0=new BufferedReader(new InputStreamReader(hdfs.open(p0)));
			colsRel=br0.readLine();
			br0.close();
			
			//ignore line with 0 offset containing column names
			conf.setBoolean("ignoreZeroLine", true);
		}else{
			conf.setBoolean("ignoreZeroLine", false);
		}
		
		//--args group-by-columns
		String[] groupByCols=groupBy.split(conf.get("delimiter"));
		String[] colPos=new String[groupByCols.length];
		for (int i=0;i<colPos.length;i++){
			colPos[i]=Integer.toString(Arrays.asList(colsRel.split(conf.get("delimiter"))).indexOf(groupByCols[i]));
		}
		conf.set("group_by_cols", StringUtils.join(colPos,conf.get("delimiter")));
		
		System.out.println("Relation cols:"+colsRel); //DBG
		System.out.println("GroupBy cols:"+groupBy); //DBG
		System.out.println("GroupBy cols pos:"+conf.get("group_by_cols")); //DBG
		
		//--job configuration--
		Job job = new Job(conf,"GroupBy");
		job.setJarByClass(MRGroupBy.class);

		//num of reducers
		job.setNumReduceTasks(reducerNum);

		//inputs
		FileInputFormat.addInputPath(job, p0);
		//output
		FileOutputFormat.setOutputPath(job, outp);
		//classes
		job.setMapperClass(GroupByMapper.class);
		job.setReducerClass(GroupByReducer.class);
		//map output
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		//reducer output
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);
		
		return job.waitForCompletion(true);
	}
	public static void main(String[] args) throws Exception {

		//correct usage check
		if (args.length !=4 ) {
			System.err.println("Usage: MRGroupBy <input path> <output path> <group_by_column_names_comma_separated> <num_of_machines>");
			System.exit(-1);
		}
		
		//input paths
		Path p0=new Path(args[0]);
		Path outp=new Path(args[1]);
		

		Configuration conf=new Configuration();
		
		//filesystem
		FileSystem hdfs=FileSystem.get(conf);
		System.out.println(hdfs.getWorkingDirectory()); //DBG
		

		System.out.println("P0: "+hdfs.exists(p0));	//DBG
		System.out.println("OutP: "+hdfs.exists(outp));	//DBG
		
		//column delimiter
		conf.set("delimiter", ",");
		
		System.exit( run(p0,outp,conf,args[2],null,Integer.parseInt(args[3]))? 
				(Utils.copyMergeWTitle(hdfs, outp, hdfs, new Path(args[1]+"merged"), false, conf, args[2]+conf.get("delimiter")+"count\n") ? 0 : 1) //if job is successful, merge outputs and append column names
				: 1);
	
	}
}
