#!/usr/bin/env jsb

public class Calculator
{
	protected JFrame f = new JFrame("calculator");
	
	protected JTextField l = new JTextField(16);
	JPanel p = new JPanel();
	
	protected String val0;
	protected String op;
	protected String val1;
	
	protected void reset()
	{
		val0 = "";
		op = "";
		val1 = "";
		showOps(null);
	}
	
	protected void showOps(String extra)
	{
		l.setText(val0 + op + val1+(null==extra?"":extra));
	}
	
	// default constrcutor 
	public Calculator() 
	{ 
		l.setEditable(false);
		reset();
		
		p.add(l);
		for
		(
			String s:new String[]
			{
				"+", "-", "*", "/",
				"^", "7", "8", "9",
				"%", "4", "5", "6",
				".", "1", "2", "3", 
				"<", "C", "0", "=",
			}
		)
		{
			JButton b = new JButton(String.valueOf(s));
			b.addActionListener(ac);
			p.add(b);
		}
		
		f.add(p);
		f.setSize(200, 220);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public void show()
	{
		f.setVisible(true);
	}
	
	protected ActionListener ac = (e)->
	{
		String s = e.getActionCommand();
		
		if((s.charAt(0) >= '0' && s.charAt(0) <= '9') || s.charAt(0) == '.')
		{
			if(!op.isEmpty())
			{
				val1 = val1 + s;
			}
			else
			{
				val0 = val0 + s;
			}
			showOps(null);
		}
		else
		{
			switch(s.charAt(0))
			{
			case 'C':
				reset();
				break;
			
			case '<':
				if(op.isEmpty() && !val0.isEmpty())
				{
					val0 = val0.substring(0, val0.length()-1);
				}
				else if(!val1.isEmpty())
				{
					val1 = val1.substring(0, val1.length()-1);
				}
				showOps(null);
				break;
				
			case '=':
				double res = 0.0;
				switch(op)
				{
				case "+":
					res = Double.parseDouble(val0) + Double.parseDouble(val1);
					break;
				case "-":
					res = Double.parseDouble(val0) - Double.parseDouble(val1);
					break;
				case "/":
					res = Double.parseDouble(val0) / Double.parseDouble(val1);
					break;
				case "*":
					res = Double.parseDouble(val0) * Double.parseDouble(val1);
					break;
				case "%":
					res = Double.parseDouble(val0) % Double.parseDouble(val1);
					break;
				case "^":
					res = Math.pow(Double.parseDouble(val0), Double.parseDouble(val1));
					break;
				
				default: return;
				}
				showOps("="+res);
				val0 = Double.toString(res);
				op = val1 = "";
				break;
			
			default:
				op = s;
				showOps(null);
				break;
			}
		}
	};
	
	public static void main(String args[]) 
	{
		new Calculator().show();;
	} 
}