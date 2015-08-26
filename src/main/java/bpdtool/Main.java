package bpdtool;

import bpdtool.codegen.ErrorLogger;
import bpdtool.codegen.Exporter;
import bpdtool.codegen.ITextWriter;
import bpdtool.data.Protocol;
import bpdtool.gui.MainFrame;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class Main implements ITextWriter
{
	private static Main s_this;

	public static void main(String args[])
	{
		s_this = new Main();

		if(args.length >= 1)
		{
			if(args.length >= 2)
			{
				if(args[0].endsWith("edit") && Util.isFileExists(args[1]))
				{
					MainFrame.launch(args[1]);
					return;
				}
			}

			if(Util.isFileExists(args[0]))
			{
				s_this.batchExport(args[0]);
				return;
			}
		}

		MainFrame.launch(null);
	}

	private void batchExport(String filename)
	{
		try
		{
			writeln("Opening Protocol XML file: " + filename);
			Protocol doc = new Protocol(filename);
			Exporter exp = new Exporter(doc);

			ErrorLogger logger = new ErrorLogger();
			if (!exp.prepare(logger))
			{
				writeln(logger.getLoggedString());
			}
			else
			{
				if (exp.export(this))
					writeln("Successfully exported.");
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			writeln("Export failed: " + ex.getMessage());
		}
	}

	@Override
	public void write(String format, Object... args)
	{
		System.out.print(Util.stringFormat(format, args));
	}

	@Override
	public void writeln(String format, Object... args)
	{
		System.out.println(Util.stringFormat(format, args));
	}

	public static ImageIcon createImageIcon(String path, String alt)
	{
		java.net.URL imgUrl = s_this.getClass().getResource("/" + path);
		if (imgUrl != null)
		{
			return new ImageIcon(imgUrl, alt);
		}

		throw new RuntimeException("Couldn't find image: " + path);
	}

	public static String getCodeTemplate(String path) throws Exception
	{
		InputStream isr = s_this.getClass().getResourceAsStream("/" + path);
		if (isr != null)
		{
			byte[] buff = new byte[isr.available()];
			isr.read(buff);
			isr.close();
			return new String(buff, "UTF-8");
		}
		else
		{
			throw new Exception("Couldn't find code template: " + path);
		}
	}
}
