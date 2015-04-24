package bpdtool.codegen;

public class ErrorLogger extends StringWriter
{
	public boolean isLogged()
	{
		return (m_sb.length() > 0);
	}

	public String getLoggedString()
	{
		return super.toString();
	}
}
