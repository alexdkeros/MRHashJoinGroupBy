import groupBy.MRGroupBy;
import hashJoin.MRHashJoin;
import helperClasses.Merger;
import helperClasses.Utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

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

		conf.addResource(hdfs.open(new Path(hdfs.getWorkingDirectory()+"/mapred-HJG.xml")));
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
		
		System.out.println("Using merger:"+conf.get("merger")); //DBG
		
		if (conf.get("merger").equals("FS")){
			//merge in filesystem
			Utils.copyMergeWTitle(hdfs,outpGB, hdfs, outpM, false, conf, args[3]+ conf.get("delimiter") + "count\n");
		}else if (conf.get("merger").equals("MR")){
			//merge using MapReduce job
			conf.set("first-line", args[3]+ conf.get("delimiter") + "count");
			exitStatus=(Merger.run(outpGB, outpM, conf)? exitStatus :1);
		}
		
		System.exit(exitStatus);
	}
}
