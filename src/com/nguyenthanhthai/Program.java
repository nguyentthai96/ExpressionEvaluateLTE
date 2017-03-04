package com.nguyenthanhthai;

import com.nguyenthanhthai.utils.Expression;

public class Program {


	
	public static void main(String[] args) {
		Expression ex=new Expression("9+12-(10+3)");
		Double result=ex.evaluate();
		System.out.println(result.doubleValue());

	}

}
