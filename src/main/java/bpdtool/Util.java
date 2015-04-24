package bpdtool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util
{
	private Util() {}

	public static void printf(String format, Object ... args)
	{
		System.out.println(args.length == 0 ? format : String.format(format, args));
	}

	public static boolean isMacOSX()
	{
		String osName = System.getProperty("os.name");	// "Mac OS X"
		return osName.contains("OS X");
	}

	public static boolean isRightMouseButton(MouseEvent e)
	{
		return (SwingUtilities.isRightMouseButton(e) || e.isControlDown());
	}

	public static void drawStringCenter(Graphics gr, String str, int x, int y)
	{
		assert (str != null);
		if(str == null)
			return;

		Rectangle rc = gr.getFontMetrics().getStringBounds(str, gr).getBounds();
		gr.drawString(str, x - (int)(rc.getWidth() / 2), y + 4);//(int)(rc.getHeight() /2));
	}

	public static Rectangle measureString(Graphics gr, String str, Font font)
	{
		return gr.getFontMetrics(font).getStringBounds(str, gr).getBounds();
	}

	public static String[] splitAndMeasureMultilineString(Graphics gr, String str, Font font, int[] cxy)
	{
		Rectangle rc = null;

		int cx = 0, cy = 0;
		String[] lines = str.split("\n");
		for(String line : lines)
		{
			rc = gr.getFontMetrics(font).getStringBounds(str, gr).getBounds();
			if(cx < rc.width)
				cx = rc.width;
			cy += rc.height;
		}

		cxy[0] = cx;
		cxy[1] = cy;
		cxy[2] = rc.height;
		return lines;
	}

	public static void drawMultilineString(Graphics gr, String[] lines, int x, int y, int cyLine)
	{
		for(String line : lines)
		{
			gr.drawString(line, x, y);
			y += cyLine;
		}
	}
/*
	public static int getComponentIndex(Container container, Component child)
	{
		int cnt = container.getComponentCount();
		while(cnt-- > 0)
		{
			if(container.getComponent(cnt) == child)
				return cnt;
		}

		return -1;
	}
*/
	public static boolean isNullOrEmpty(String str)
	{
		return (str == null || str.isEmpty());
	}

	public static boolean isStringTrue(String val)
	{
		if (val == null)
			return false;

		return (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes") || val.equalsIgnoreCase("1"));
	}

	private static Pattern re_VarChars = Pattern.compile("^[a-zA-Z_]+\\w*$");
	public static boolean isValidVarName(String name)
	{
		return re_VarChars.matcher(name).matches();
	}

	private static Pattern re_Number = Pattern.compile("^[0-9]+$");
	private static Pattern re_HexNumber = Pattern.compile("^0[xX]{1}[0-9A-Fa-f]+$");
	public static boolean isStringNumber(String str)
	{
		return (re_Number.matcher(str).matches()
				|| re_HexNumber.matcher(str).matches());
	}

	public static int getStringNumber(String str)
	{
		if (re_Number.matcher(str).matches())
		{
			return Integer.parseInt(str);
		}
		else if (re_HexNumber.matcher(str).matches())
		{
			return Integer.parseInt(str.substring(2), 16);
		}

		throw new RuntimeException("Invalid number: " + str);
	}

	public static String stringJoin(String sep, Object[] arr)
	{
		StringBuilder sb = new StringBuilder();
		for(Object o : arr)
		{
			if(o instanceof String)
			{
				String s = o.toString();

				if(sb.length() > 0)
					sb.append(sep);
				sb.append(s);
			}
		}
		return sb.toString();
	}

	private static Pattern re_ParamHolder = Pattern.compile("\\{(\\d+)\\}");
	public static String stringFormat(String fmt, Object... args)
	{
		if(args.length == 0)
			return fmt;

		StringBuilder sb = new StringBuilder();
		int isb = 0;
		Matcher m = re_ParamHolder.matcher(fmt);
		while(m.find())
		{
			int start = m.start();
			int index = Integer.parseInt(m.group(1));

			if(isb < start)
				sb.append(fmt.substring(isb, m.start()));
			sb.append(args[index]);
			isb = m.end();
		}

		if(isb < fmt.length())
			sb.append(fmt.substring(isb));

		return sb.toString();
	}

	public static boolean tryParseInt(String str, int[] ret)
	{
		try
		{
			ret[0] = Integer.parseInt(str);
			return true;
		}
		catch(Exception ex)
		{
			return false;
		}
	}

	public static String filterNewlineString(String str)
	{
		int b = 0;
		str = str.toUpperCase();
		if(str.contains("CR"))
			b |= 1;
		if(str.contains("LF"))
			b |= 2;

		switch(b)
		{
		case 1:
			return "CR";
		case 2:
			return "LF";
		default:
			return "CR+LF";
		}
	}

	public static String capitalize(String str)
	{
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	public static boolean isFileExists(String pathname)
	{
		try
		{
			File f = new File(pathname);
			return f.exists();
		}
		catch(Exception ex)
		{
			return false;
		}
	}

	public static boolean isDirectoryExists(String pathname)
	{
		if(pathname.isEmpty() || pathname.equals("."))
			return true;

		return isFileExists(pathname);
	}

	public static boolean deleteFile(String pathname)
	{
		try
		{
			File f = new File(pathname);
			if(!f.exists())
				return true;

			return f.delete();
		}
		catch(Exception ex)
		{
			return false;
		}
	}

	public static boolean renameFile(String fn1, String fn2)
	{
		try
		{
			File f1 = new File(fn1);
			File f2 = new File(fn2);

			return f1.renameTo(f2);
		}
		catch(Exception ex)
		{
			return false;
		}
	}

	public static String getCurrentDirectory()
	{
		try
		{
			File f = new File(".");
			return f.getCanonicalPath();
		}
		catch (IOException ex)
		{
			return "";
		}
	}

	public static String getDirectoryName(String filename)
	{
		int idx = filename.replace('\\', '/').lastIndexOf('/');
		if(idx >= 0)
		{
			String ret = filename.substring(0, idx);
			return (ret.indexOf('/') < 0 && ret.indexOf('\\') < 0) ? (ret + '/') : ret;
		}

		return "";
	}

	public static String pathCombine(String base, String add)
	{
		if(base.isEmpty())
			return add;

		if(base.endsWith("/") || base.endsWith("\\"))
		{
			return base + add;
		}
		else
		{
			return base + File.separator + add;
		}
	}

}
