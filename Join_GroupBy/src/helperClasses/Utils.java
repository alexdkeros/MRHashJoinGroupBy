package helperClasses;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

public class Utils {
	
	
	/** Copy all files in a directory to one output file(merge), append to start of file a custom string. 
	 * (edit of org/apache/hadoop/fs/FileUtil.html#copyMerge(org.apache.hadoop.fs.FileSystem,
	 * 														org.apache.hadoop.fs.Path,
	 * 														org.apache.hadoop.fs.FileSystem,
	 * 														org.apache.hadoop.fs.Path,
	 * 														boolean,
	 * 														org.apache.hadoop.conf.Configuration,
	 * 														java.lang.String)
	 */
	public static boolean copyMergeWTitle(FileSystem srcFS, Path srcDir,
			FileSystem dstFS, Path dstFile, boolean deleteSource,
			Configuration conf, String startString) throws IOException {
		dstFile = checkDest(srcDir.getName(), dstFS, dstFile, false);

		if (!srcFS.getFileStatus(srcDir).isDir())
			return false;

		OutputStream out = dstFS.create(dstFile);

		try {
			//add string first
			if (startString != null)
				out.write(startString.getBytes("UTF-8"));
			
			FileStatus contents[] = srcFS.listStatus(srcDir);
			Arrays.sort(contents);
			for (int i = 0; i < contents.length; i++) {
				if (!contents[i].isDir()) {
					InputStream in = srcFS.open(contents[i].getPath());
					try {
						
						IOUtils.copyBytes(in, out, conf, false);

					} finally {
						in.close();
					}
				}
			}
		} finally {
			out.close();
		}

		if (deleteSource) {
			return srcFS.delete(srcDir, true);
		} else {
			return true;
		}
	}
	
	private static Path checkDest(String srcName, FileSystem dstFS, Path dst,
			boolean overwrite) throws IOException {
		if (dstFS.exists(dst)) {
			FileStatus sdst = dstFS.getFileStatus(dst);
			if (sdst.isDir()) {
				if (null == srcName) {
					throw new IOException("Target " + dst + " is a directory");
				}
				return checkDest(null, dstFS, new Path(dst, srcName), overwrite);
			} else if (!overwrite) {
				throw new IOException("Target " + dst + " already exists");
			}
		}
		return dst;
	}
	
	public static String relationColumn(String relName, String cols,String splitDelim,String betweenDelim){
		String[] colArray=cols.split(splitDelim);
		String[] outArray=new String[colArray.length];
		for (int i=0;i<colArray.length;i++){
			outArray[i]=relName+betweenDelim+colArray[i];
		}
		return StringUtils.join(outArray,splitDelim);	
	}

	
}