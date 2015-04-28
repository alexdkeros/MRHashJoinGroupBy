package hashJoin;

import helperClasses.TextPair;
import helperClasses.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.google.common.collect.HashMultimap;

public class MRHashJoin{
	
	/*
	 * value counter
	 */
	static enum JoinVals{
		NULL
	}
	
	/*
	 * Mapper
	 */
	public static class HashJoinMapper extends Mapper<LongWritable, Text, TextPair, Text> {


		private TextPair outKey=new TextPair();

		//custom parameters
		private String delim;
		private int columnPos;
		private String relation;

		@Override
		public void setup(Context context){

			Configuration conf=context.getConfiguration();

			//get filename this inputSplit came from
			relation=((FileSplit)context.getInputSplit()).getPath().getName();

			//get column position from configuration
			columnPos=conf.getInt(relation+"_join_pos", -1);

			//get delimiter from configuration
			delim=conf.get("delimiter");
		}
		
		@Override
		public void map(LongWritable key, Text value, Context context) 
				throws IOException, InterruptedException {
			
			System.out.println("MAP key:"+key+" val:"+value);//DBG
			
			if (!(key.equals(new LongWritable(0)) && context.getConfiguration().getBoolean("ignoreZeroLine", true))){ //ignore first line of file containing column names

				String[] values=(value.toString()).split(delim);
								
				if (!values[columnPos].equals("null")){ //ignore rows with null value at join column

					outKey.set(values[columnPos],relation);

					context.write(outKey,value);
				}else{
					context.getCounter(JoinVals.NULL).increment(1);
				}
			}
		}

	}
	
	/*
	 * Partitioner
	 */
	public static class FirstPartitioner extends Partitioner<TextPair, Text> {

		@Override
		public int getPartition(TextPair key, Text value, int numPartitions) {
			return Math.abs(key.getFirst().hashCode() * 127) % numPartitions;

		}
	}
	
	/*
	 * Key comparator
	 */
	public static class KeyComparator extends WritableComparator {
		protected KeyComparator() {
			super(TextPair.class, true);
		}
		@Override
		public int compare(WritableComparable w1, WritableComparable w2) {
			TextPair tp1 = (TextPair) w1;
			TextPair tp2 = (TextPair) w2;
			return tp1.getSecond().compareTo(tp2.getSecond());
		}
	}

	
	/*
	 * Group comparator
	 */
	public static class GroupComparator extends WritableComparator {
		protected GroupComparator() {
			super(TextPair.class, true);
		}
		
		@Override
		public int compare(WritableComparable w1, WritableComparable w2) {
			TextPair ip1 = (TextPair) w1;
			TextPair ip2 = (TextPair) w2;
			return ip1.getSecond().compareTo(ip2.getSecond());
		}
	}

	
	/*
	 * Reducer
	 */
	public static class HashJoinReducer extends Reducer<TextPair, Text, NullWritable, Text> {

		private static HashMultimap<String,String> hmap;
		private String delim;
		
		@Override
		public void setup(Context context){

			Configuration conf=context.getConfiguration();

			//get delimiter from configuration
			delim=conf.get("delimiter");
			
			hmap=HashMultimap.create();
			
		}
		
		@Override
		public void reduce(TextPair key, Iterable<Text> values,Context context) 
				throws IOException, InterruptedException {

			int joinPos=context.getConfiguration().getInt(key.getSecond()+"_join_pos", -1);
			
 
			if (hmap.isEmpty()){
				//populate hash map
				
				int i=0; //DBG
				for (Iterator<Text> iterator = values.iterator(); iterator.hasNext();) {
					Text value = (Text) iterator.next();					

					System.out.println("RED populate inc:"+(i++)+" key:"+key+" val:"+value); //DBG

					
					String[] attrs=(value.toString()).split(delim);
					//hash by join column, remove column from rest of tuple
					hmap.put(attrs[joinPos], StringUtils.join(ArrayUtils.remove(attrs, joinPos),delim)); //can use ArrayUtils.remove(attrs, joinPos) to remove joining column

					System.out.println("RED hmap populate:"+hmap); //DBG

				}
			}else{
				//probe hash map

				System.out.println("RED hmap probe:"+hmap); //DBG
				int i=0; //DBG
				
				for (Iterator<Text> iterator = values.iterator(); iterator.hasNext();) {
					Text value = (Text) iterator.next();

					System.out.println("RED probe inc:"+(i++)+" key:"+key+" val:"+value); //DBG

					String[] attrs=(value.toString()).split(delim);
					
					Set<String> others=hmap.get(attrs[joinPos]);
					if (!others.isEmpty()){
						for (String o : others){
							context.write(NullWritable.get(), new Text(attrs[joinPos]+context.getConfiguration().get("delimiter")+ //joining attribute
																		o+context.getConfiguration().get("delimiter")+ //relation in hash table
																		StringUtils.join(ArrayUtils.remove(attrs, joinPos),delim)));  //probing relation
												
						}
					}
				}
			}
			
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		
		//correct usage check
		if (args.length != 5) {
			System.err.println("Usage: MRHashJoin <input path 1> <input path 2> <output path> <join_column_name> <num_of_machines>");
			System.exit(-1);
		}
		
		System.out.println("Path0:"+args[0]); //DBG
		System.out.println("Path1:"+args[1]); //DBG
		System.out.println("OutPath:"+args[2]); //DBG
		
		
		//input paths
		Path p0=new Path(args[0]);
		Path p1=new Path(args[1]);
		Path outp=new Path(args[2]);
		
		Configuration conf=new Configuration();
		
		//filesystem
		FileSystem hdfs=FileSystem.get(conf);
		System.out.println(hdfs.getWorkingDirectory()); //DBG
		

		System.out.println("P0: "+hdfs.exists(p0));	//DBG
		System.out.println("P1: "+hdfs.exists(p1));	//DBG
		System.out.println("OutP: "+hdfs.exists(outp));	//DBG
		
		
		//column delimiter
		conf.set("delimiter", ",");
		
		//ignore line with 0 offset containing column names
		conf.setBoolean("ignoreZeroLine", true);
		
		//--relation 0--
		//get column names
		BufferedReader br0=new BufferedReader(new InputStreamReader(hdfs.open(p0)));
		String cols0=br0.readLine();
		br0.close();
		
		//set join position for relation 0
		conf.setInt(p0.getName()+"_join_pos",Arrays.asList(cols0.split(conf.get("delimiter"))).indexOf(args[3]));
		//output columns
		String outCols0=StringUtils.join(ArrayUtils.remove(cols0.split(conf.get("delimiter")), conf.getInt(p0.getName()+"_join_pos", -1)),conf.get("delimiter"));
		
		//--relation 1--
		//get column names
		BufferedReader br1=new BufferedReader(new InputStreamReader(hdfs.open(p1)));
		String cols1=br1.readLine();
		br1.close();
		
		//set join position for relation 1
		conf.setInt(p1.getName()+"_join_pos",Arrays.asList(cols1.split(conf.get("delimiter"))).indexOf(args[3]));
		//output columns
		String outCols1=StringUtils.join(ArrayUtils.remove(cols1.split(conf.get("delimiter")), conf.getInt(p1.getName()+"_join_pos", -1)),conf.get("delimiter"));
				
		//join columns
		String joinCols=args[3]+"(joinAttr)"+conf.get("delimiter")+
				Utils.relationColumn(FilenameUtils.removeExtension(p0.getName()),outCols0,conf.get("delimiter"),".")+
				conf.get("delimiter")+
				Utils.relationColumn(FilenameUtils.removeExtension(p1.getName()),outCols1,conf.get("delimiter"),".")+"\n";
		
		//--job configuration--
		Job job = new Job(conf,"HashJoin");
		job.setJarByClass(MRHashJoin.class);
		
		//num of reducers
		job.setNumReduceTasks(Integer.parseInt(args[4]));
		
		//inputs
		FileInputFormat.addInputPath(job, p0);
		FileInputFormat.addInputPath(job, p1);
		//output
		FileOutputFormat.setOutputPath(job, outp);
		//classes
		job.setMapperClass(HashJoinMapper.class);
		job.setPartitionerClass(FirstPartitioner.class);
		job.setSortComparatorClass(KeyComparator.class);
		job.setGroupingComparatorClass(GroupComparator.class);
		job.setReducerClass(HashJoinReducer.class);
		//map output
		job.setMapOutputKeyClass(TextPair.class);
		job.setMapOutputValueClass(Text.class);
		//reducer output
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);

		System.exit(job.waitForCompletion(true) ? 
				(Utils.copyMergeWTitle(hdfs, outp, hdfs, new Path(args[2]+"merged"), false, conf, joinCols) ? 0 : 1) //if job is successful, merge outputs and append column names
				: 1);
	}
	
	

}