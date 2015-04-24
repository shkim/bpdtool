package bpdtool;

import bpdtool.codegen.ErrorLogger;
import bpdtool.codegen.Exporter;
import bpdtool.codegen.ITextWriter;
import bpdtool.data.Protocol;
import bpdtool.gui.MainFrame;

public class Main implements ITextWriter
{
	public static void main(String args[])
	{
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
				batchExport(args[0]);
				return;
			}
		}

		MainFrame.launch(null);
	}

	static void batchExport(String filename)
	{
		Main consoleOut = new Main();

		try
		{
			consoleOut.writeln("Opening Protocol XML file: " + filename);
			Protocol doc = new Protocol(filename);
			Exporter exp = new Exporter(doc);

			ErrorLogger logger = new ErrorLogger();
			if (!exp.prepare(logger))
			{
				consoleOut.writeln(logger.getLoggedString());
			}
			else
			{
				if (exp.export(consoleOut))
					consoleOut.writeln("Successfully exported.");
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			consoleOut.writeln("Export failed: " + ex.getMessage());
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
}
