package bpdtool.codegen;

public interface ITextWriter
{
	void write(String format, Object ... args);
	void writeln(String format, Object ... args);
}
