import groupBy.MRGroupBy;
import groupBy.MRGroupBy.GroupByMapper;
import groupBy.MRGroupBy.GroupByReducer;
import hashJoin.MRHashJoin.*;
import hashJoin.MRHashJoin;
import hashJoin.MRHashJoin.FirstPartitioner;
import hashJoin.MRHashJoin.GroupComparator;
import hashJoin.MRHashJoin.HashJoinMapper;
import hashJoin.MRHashJoin.HashJoinReducer;
import hashJoin.MRHashJoin.KeyComparator;
import helperClasses.TextPair;
import helperClasses.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class MRHashJoinGroup {

	public static void main(String[] args) throws Exception {

		// correct usage check
		if (args.length != 5) {
			System.err
					.println("Usage: MRHashJoinGroup <path_to_left_table> <path_to_right_table> <name_of_join_column> <group_by_column_names_comma_separated> <num_of_machines>");
			System.exit(-1);
		}

		System.out.println("Path0:" + args[0]); // DBG
		System.out.println("Path1:" + args[1]); // DBG

		// input paths
		Path p0 = new Path(args[0]);
		Path p1 = new Path(args[1]);

		// -----------------------------------------------HASHJOIN-------------------------------------------

		Configuration confJoin = new Configuration();

		// column delimiter
		confJoin.set("delimiter", ",");
		// ignore line with 0 offset containing column names
		confJoin.setBoolean("ignoreZeroLine", true);

		// filesystem
		FileSystem hdfs = FileSystem.get(confJoin);
		System.out.println(hdfs.getWorkingDirectory()); // DBG

		System.out.println("P0: " + hdfs.exists(p0)); // DBG
		System.out.println("P1: " + hdfs.exists(p1)); // DBG

		// output path
		Path outp = new Path(hdfs.getWorkingDirectory().toString() + "/outJoin");

		// --relation 0--
		// get column names
		BufferedReader br0 = new BufferedReader(new InputStreamReader(
				hdfs.open(p0)));
		String cols0 = br0.readLine();
		br0.close();

		// set join position for relation 0
		confJoin.setInt(
				p0.getName() + "_join_pos",
				Arrays.asList(cols0.split(confJoin.get("delimiter"))).indexOf(
						args[2]));

		// --relation 1--
		// get column names
		BufferedReader br1 = new BufferedReader(new InputStreamReader(
				hdfs.open(p1)));
		String cols1 = br1.readLine();
		br1.close();

		// set join position for relation 0
		confJoin.setInt(
				p1.getName() + "_join_pos",
				Arrays.asList(cols1.split(confJoin.get("delimiter"))).indexOf(
						args[2]));

		// join columns
		String joinCols = Utils.relationColumn(
				FilenameUtils.removeExtension(p0.getName()), cols0,
				confJoin.get("delimiter"), ".")
				+ confJoin.get("delimiter")
				+ Utils.relationColumn(
						FilenameUtils.removeExtension(p1.getName()), cols1,
						confJoin.get("delimiter"), ".");

		// --job configuration--
		Job jobJoin = new Job(confJoin, "HashJoin");
		jobJoin.setJarByClass(MRHashJoin.class);

		// num of reducers
		jobJoin.setNumReduceTasks(Integer.parseInt(args[4]));

		// inputs
		FileInputFormat.addInputPath(jobJoin, p0);
		FileInputFormat.addInputPath(jobJoin, p1);
		// output
		FileOutputFormat.setOutputPath(jobJoin, outp);
		// classes
		jobJoin.setMapperClass(HashJoinMapper.class);
		jobJoin.setPartitionerClass(FirstPartitioner.class);
		jobJoin.setSortComparatorClass(KeyComparator.class);
		jobJoin.setGroupingComparatorClass(GroupComparator.class);
		jobJoin.setReducerClass(HashJoinReducer.class);
		// map output
		jobJoin.setMapOutputKeyClass(TextPair.class);
		jobJoin.setMapOutputValueClass(Text.class);
		// reducer output
		jobJoin.setOutputKeyClass(NullWritable.class);
		jobJoin.setOutputValueClass(Text.class);

		jobJoin.waitForCompletion(true);

		// --------------------------------------------------------------------------------------------------

		// -----------------------------------------------GROUPBY-------------------------------------------

		// output path
		Path outpAll = new Path(hdfs.getWorkingDirectory() + "/outHJG");

		Configuration confGroup = new Configuration();

		// column delimiter
		confGroup.set("delimiter", ",");

		// ignore line with 0 offset containing column names
		confGroup.setBoolean("ignoreZeroLine", false);

		String colsRel = joinCols;

		// --args group-by-columns
		String[] groupByCols = args[3].split(confGroup.get("delimiter"));
		String[] colPos = new String[groupByCols.length];
		for (int i = 0; i < groupByCols.length; i++) {
			colPos[i] = Integer.toString(Arrays.asList(
					colsRel.split(confGroup.get("delimiter"))).indexOf(
					groupByCols[i]));
			
			System.out.println("Finding col pos:"+Integer.toString(Arrays.asList(
					colsRel.split(confGroup.get("delimiter"))).indexOf(
					groupByCols[i]))); //DBG
		}
		confGroup.set("group_by_cols",
				StringUtils.join(colPos, confGroup.get("delimiter")));
		
		System.out.println("From args:"+StringUtils.join(groupByCols,",")); //DBG
		System.out.println("Relation cols:" + colsRel); // DBG
		System.out.println("GroupBy cols:" + args[3]); // DBG
		System.out.println("GroupBy cols pos:" + confGroup.get("group_by_cols")); // DBG

		// --job configuration--
		Job job = new Job(confGroup, "GroupBy");
		job.setJarByClass(MRGroupBy.class);

		// num of reducers
		job.setNumReduceTasks(Integer.parseInt(args[4]));

		// inputs
		FileInputFormat.addInputPath(job, outp);
		// output
		FileOutputFormat.setOutputPath(job, outpAll);
		// classes
		job.setMapperClass(GroupByMapper.class);
		job.setReducerClass(GroupByReducer.class);
		// map output
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		// reducer output
		job.setOutputKeyClass(NullWritable.class);
		job.setOutputValueClass(Text.class);

		System.exit(job.waitForCompletion(true) ? (Utils.copyMergeWTitle(hdfs,
				outpAll, hdfs, new Path(hdfs.getWorkingDirectory() + "/outHJG/merged"), false, confGroup, args[3]
						+ confGroup.get("delimiter") + "count\n") ? 0 : 1) // if job
																		// is
																		// successful,
																		// merge
																		// outputs
																		// and
																		// append
																		// column
																		// names
				: 1);

	}
}
