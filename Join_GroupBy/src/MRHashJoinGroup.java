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
import helperClasses.Merger;
import helperClasses.TextPair;
import helperClasses.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
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

		int exitStatus=0;
		
		// correct usage check
		if (args.length != 5) {
			System.err
					.println("Usage: MRHashJoinGroup <path_to_left_table> <path_to_right_table> <name_of_join_column> <group_by_column_names_comma_separated> <num_of_machines>");
			System.exit(-1);
		}

		System.out.println("Path0:" + args[0]); // DBG
		System.out.println("Path1:" + args[1]); // DBG
		
		int reducerNum=Integer.parseInt(args[4]);

		Configuration conf=new Configuration();
		
		//filesystem
		FileSystem hdfs=FileSystem.get(conf);
		System.out.println(hdfs.getWorkingDirectory()); //DBG
		
		//column delimiter
		conf.set("delimiter", ",");
		//ignore line with 0 offset containing column names
		conf.setBoolean("ignoreZeroLine", true);
		//attach relation names to join cols
		conf.setBoolean("attach-relation-names", false);
		//hashJoin output folder
		conf.set("hashjoin-out", "outHJ");
		conf.set("groupby-out","outGB");
		conf.set("merge-out","outHJG/merged");
		conf.set("merger","fs"); //fs or MR
		
		// -----------------------------------------------HASHJOIN-------------------------------------------

		// input paths
		Path p0 = new Path(args[0]);
		Path p1 = new Path(args[1]);
		//output path
		Path outpHJ=new Path(conf.get("hashjoin-out"));
		//run
		exitStatus=(MRHashJoin.run(p0, p1, outpHJ, args[2], conf, reducerNum)? exitStatus :1);

		// --------------------------------------------------------------------------------------------------

		// -----------------------------------------------GROUPBY-------------------------------------------
		//output path
		Path outpGB=new Path(conf.get("groupby-out"));
		//run
		exitStatus=(MRGroupBy.run(outpHJ, outpGB, conf, args[3],conf.get("output-join-cols"), reducerNum)? exitStatus :1);
		// --------------------------------------------------------------------------------------------------

		// -----------------------------------------------MERGE-------------------------------------------
		//merge path
		Path outpM=new Path(conf.get("merge-out"));
		
		if (conf.get("merger")=="fs"){
			//merge in filesystem
			Utils.copyMergeWTitle(hdfs,outpGB, hdfs, outpM, false, conf, args[3]+ conf.get("delimiter") + "count\n");
		}else if (conf.get("merger")=="mr"){
			//merge using MapReduce job
			conf.set("first-line", args[3]+ conf.get("delimiter") + "count");
			exitStatus=(Merger.run(outpGB, outpM, conf)? exitStatus :1);
		}
		
		System.exit(exitStatus);
	}
}
