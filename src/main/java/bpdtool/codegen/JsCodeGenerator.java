package bpdtool.codegen;

import bpdtool.data.Protocol;

public class JsCodeGenerator extends CodeGenerator
{
	@Override
	public boolean prepare(ITextWriter logger, Protocol doc)
	{
		logger.writeln("TODO: JsPrepare");
		return false;
	}

	@Override
	public boolean export(ITextWriter logger) throws Exception
	{
		logger.writeln("TODO: JsExport");
		return false;
	}
}
