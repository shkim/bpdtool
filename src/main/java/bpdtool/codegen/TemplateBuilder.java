package bpdtool.codegen;

import bpdtool.Util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateBuilder
{
	private StringBuilder m_buffer;

	public TemplateBuilder(String templ)
	{
		m_buffer = new StringBuilder(templ);

		// regularize CR,LF
		while(true)
		{
			int start = m_buffer.indexOf("\r");
			if(start < 0)
				break;

			if (start + 1 < m_buffer.length())
			{
				if (m_buffer.charAt(start +1) == '\n')
				{
					m_buffer.deleteCharAt(start);
					continue;
				}
			}

			m_buffer.setCharAt(start, '\n');
		}
	}

	private static Pattern re_IF = Pattern.compile("###IF<(\\w+)>###");

	private static class ConditionBlock
	{
		public int start;
		public int end;
		public String condition;
		public int bodyStart;
		public int bodyEnd;
	}

	public void selectCondition(ArrayList<String> conditions)
	{
		ArrayList<ConditionBlock> blocks = new ArrayList<>();

		int iBegin = 0;
		Matcher m = re_IF.matcher(m_buffer.toString());
		while(m.find(iBegin))
		{
			ConditionBlock blk = new ConditionBlock();
			blk.start = m.start();
			blk.condition = m.group(1);
			blk.bodyStart = m_buffer.indexOf("\n", m.end()) +1;

			String endMarker = String.format("###ENDIF<%s>###", blk.condition);
			blk.bodyEnd = m_buffer.indexOf(endMarker, blk.start);
			if (blk.bodyEnd < 0)
			{
				throw new RuntimeException("Block ending marker not found: " + endMarker);
			}

			blk.end = m_buffer.indexOf("\n", blk.bodyEnd);
			if (blk.end < 0)
				blk.end = blk.bodyEnd + endMarker.length();
			else
				blk.end++;

			blocks.add(blk);
			iBegin = blk.end;
		}

		for (int iBlock = blocks.size(); iBlock > 0;)
		{
			ConditionBlock blk = blocks.get(--iBlock);

			if (conditions.contains(blk.condition))
			{
				String body = m_buffer.substring(blk.bodyStart, blk.bodyEnd);
				m_buffer.replace(blk.start, blk.end, body);
			}
			else
			{
				m_buffer.delete(blk.start, blk.end);
			}
		}
	}

	public void selectCondition(String condition)
	{
		ArrayList<String> arr = new ArrayList<>(1);
		arr.add(condition);
		selectCondition(arr);
	}

	public void substitute(String marker, String str)
	{
		while(true)
		{
			int start = m_buffer.indexOf(marker);
			if(start < 0)
				break;

			int end = start + marker.length();
			if (end < m_buffer.length() && m_buffer.charAt(end) == '\n')
			{
				end++;
			}

			m_buffer = m_buffer.replace(start, end, str);
		}
	}

	public String getResult()
	{
		return m_buffer.toString();
	}
}
