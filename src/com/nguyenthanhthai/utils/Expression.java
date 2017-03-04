package com.nguyenthanhthai.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.nguyenthanhthai.tokens.NumberToken;
import com.nguyenthanhthai.tokens.OperatorToken;
import com.nguyenthanhthai.tokens.Token;
import com.nguyenthanhthai.tokens.Tokenizer;
import com.nguyenthanhthai.tokens.operator.Operator;

public class Expression {

    private final Token[] tokens;

    public Expression(String expression) {
        if (expression == null || expression.trim().length() == 0) {
            throw new IllegalArgumentException("Expression can not be empty");
        }

        final Map<String, Operator> userOperators;

        final Set<String> variableNames;
        
        userOperators = new HashMap<String, Operator>(4);    
        variableNames = new HashSet<String>(4);
        
        
        if (expression.length() == 0) {
            throw new IllegalArgumentException("The expression can not be empty");
        }
       
        
       this.tokens= convertToRPN(expression, userOperators, variableNames);
    }
    
    //Chuyển biểu thức thành hậy tự thông qua ánh xạ kiểu của các token 
     private Token[] convertToRPN(final String expression, 
            final Map<String, Operator> userOperators, final Set<String> variableNames){

        final Stack<Token> stack = new Stack<Token>();
        final List<Token> output = new ArrayList<Token>();

        final Tokenizer tokenizer = new Tokenizer(expression, userOperators, false);
        while (tokenizer.hasNext()) {
            Token token = tokenizer.nextToken(); //khi gọi nextTeoken nó sẽ chạy hết một loại token và 
            //trả về một ánh xạ token bao gồm kiểu và giá trị nếu là toán tử sẽ có độ ưu tiên precedence, theo class Operators	

            switch (token.getType()) {
            case Token.TOKEN_NUMBER:
            	output.add(token); 
                break;
            case Token.TOKEN_SEPARATOR:
                while (!stack.empty() && stack.peek().getType() != Token.TOKEN_PARENTHESES_OPEN) {
                    output.add(stack.pop());
                }
                if (stack.empty() || stack.peek().getType() != Token.TOKEN_PARENTHESES_OPEN) {
                    throw new IllegalArgumentException("Misplaced function separator ',' or mismatched parentheses");
                }
                break;
            case Token.TOKEN_OPERATOR:
                while (!stack.empty() && stack.peek().getType() == Token.TOKEN_OPERATOR) {
                    OperatorToken o1 = (OperatorToken) token;
                    OperatorToken o2 = (OperatorToken) stack.peek();
                    if (o1.getOperator().getNumOperands() == 1 && o2.getOperator().getNumOperands() == 2) {
                        break;
                    } else if ((o1.getOperator().isLeftAssociative() && o1.getOperator().getPrecedence() <= o2.getOperator().getPrecedence())
                            || (o1.getOperator().getPrecedence() < o2.getOperator().getPrecedence())) {
                        output.add(stack.pop());
                    }else {
                        break;
                    }
                }
                stack.push(token);
                break;
            case Token.TOKEN_PARENTHESES_OPEN:
                stack.push(token);
                break;
            case Token.TOKEN_PARENTHESES_CLOSE:
                while (stack.peek().getType() != Token.TOKEN_PARENTHESES_OPEN) {
                    output.add(stack.pop());
                }
                stack.pop();
                break;
            default:
                throw new IllegalArgumentException("Unknown Token type encountered. This should not happen");
            }
        }
        while (!stack.empty()) {
            Token t = stack.pop();
            if (t.getType() == Token.TOKEN_PARENTHESES_CLOSE || t.getType() == Token.TOKEN_PARENTHESES_OPEN) {
                throw new IllegalArgumentException("Mismatched parentheses detected. Please check the expression");
            } else {
                output.add(t);
            }
        }
        return (Token[]) output.toArray(new Token[output.size()]);
    }


    public Future<Double> evaluateAsync(ExecutorService executor) {
        return executor.submit(new Callable<Double>() {
            @Override
            public Double call() throws Exception {
                return evaluate();
            }
        });
    }

    //Thực hiện tính toán lại expresion đã được dịch về hậu tự
    public double evaluate() {
        final ArrayStack output = new ArrayStack();
        for (int i = 0; i < tokens.length; i++) {
            Token t = tokens[i];
            if (t.getType() == Token.TOKEN_NUMBER) {
                output.push(((NumberToken) t).getValue());
            } else if (t.getType() == Token.TOKEN_OPERATOR) {
                OperatorToken op = (OperatorToken) t;
                if (output.size() < op.getOperator().getNumOperands()) {
                    throw new IllegalArgumentException("Invalid number of operands available for '" + op.getOperator().getSymbol() + "' operator");
                }
                if (op.getOperator().getNumOperands() == 2) {
                    /* pop the operands and push the result of the operation */
                    double rightArg = output.pop();
                    double leftArg = output.pop();
                    output.push(op.getOperator().apply(leftArg, rightArg));
                } else if (op.getOperator().getNumOperands() == 1) {
                    /* pop the operand and push the result of the operation */
                    double arg = output.pop();
                    output.push(op.getOperator().apply(arg));
                }
            }
        }
        if (output.size() > 1) {
            throw new IllegalArgumentException("Invalid number of items on the output queue. Might be caused by an invalid number of arguments for a function.");
        }
        return output.pop();
    }
}
