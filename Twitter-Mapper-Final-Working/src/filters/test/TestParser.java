package filters.test;

import filters.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the parser.
 */
public class  TestParser {
    @Test
    public void testBasic() throws SyntaxError {
        Filter f = new Parser("trump").parse();
        assertTrue(f instanceof BasicFilter);
        assertTrue(((BasicFilter)f).getWord().equals("trump"));
    }
    @Test
    public void testOr() throws SyntaxError {
        Filter x = new Parser("kejriwal or modi").parse();
        assertTrue(x.toString().equals("(kejriwal or modi)"));
        assertTrue(x instanceof OrFilter);

    }
    @Test
    public void testAnd() throws SyntaxError {
        Filter x = new Parser("kejriwal and modi").parse();
        assertTrue(x.toString().equals("(kejriwal and modi)"));
        assertTrue(x instanceof AndFilter);

    }
    @Test
    public void testAndOr() throws SyntaxError {
        Filter x = new Parser("kejriwal and modi or rahul").parse();
        assertTrue(x.toString().equals("((kejriwal and modi) or rahul)"));


    }

    @Test
    public void testHairy() throws SyntaxError {
        Filter x = new Parser("trump and (evil or blue) and red or green and not not purple").parse();
        assertTrue(x.toString().equals("(((trump and (evil or blue)) and red) or (green and not not purple))"));
    }
}
