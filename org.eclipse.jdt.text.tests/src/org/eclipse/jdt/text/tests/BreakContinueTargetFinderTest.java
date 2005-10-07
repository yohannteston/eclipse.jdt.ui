package org.eclipse.jdt.text.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.corext.SourceRange;
import org.eclipse.jdt.internal.ui.search.BreakContinueTargetFinder;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

/**
 * Tests for the BreakContinueTargerFinder class.
 *
 * @since 3.2
 */
public class BreakContinueTargetFinderTest extends TestCase{
	private static final Class THIS= BreakContinueTargetFinderTest.class;
	
	public static Test suite() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	private ASTParser fParser;
	private BreakContinueTargetFinder fFinder;
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		fParser = ASTParser.newParser(AST.JLS3);
		fFinder= new BreakContinueTargetFinder();
		
		fJProject1= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}

	private List/*ASTNode*/ getHighlights(StringBuffer source, int offset, int length) throws Exception {
		CompilationUnit root = createCompilationUnit(source);
		String errorString = fFinder.initialize(root, offset, length);
		assertNull(errorString, errorString);
		return fFinder.perform();
	}

	private CompilationUnit createCompilationUnit(StringBuffer source) throws JavaModelException {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", source.toString(), false, null);
		fParser.setSource(cu);
		return (CompilationUnit) fParser.createAST(null);
	}

	private void checkSelection(StringBuffer s, int offset, int length, ISourceRange[] expected) throws Exception {
		List/*ASTNode*/ selectedNodes = makeSortableCopy(getHighlights(s, offset, length));
		assertEquals("number of selections", expected.length, selectedNodes.size());
		sortByStartIndex(selectedNodes);
		sortByOffset(expected);
		for (int i=0; i < selectedNodes.size(); i++) {
			ASTNode selected = (ASTNode) selectedNodes.get(i);
			ISourceRange expectedRange= expected[i];
			assertEquals(expectedRange, new SourceRange(selected));
		}
	}

	private List makeSortableCopy(List list) {
		return new ArrayList(list);
	}

	private void sortByOffset(ISourceRange[] expected) {
		reverse(SourceRange.reverseSortByOffset(expected));
	}

	private Object[] reverse(Object[] array) {
		List list = Arrays.asList(array);
		Collections.reverse(list);
		return list.toArray();
	}

	private void sortByStartIndex(List/*ASTNode*/ nodes) {
		Collections.sort(nodes, new Comparator(){
			public int compare(Object arg0, Object arg1) {
				ASTNode node0= (ASTNode) arg0;
				ASTNode node1= (ASTNode) arg1;
				return node0.getStartPosition() - node1.getStartPosition();
			}
		});
	}

	//pattern must be found - otherwise it's assumed to be an error
	private ISourceRange find(StringBuffer s, String pattern, int ithOccurrence) {
		if (ithOccurrence < 1)
			throw new IllegalStateException("ithOccurrence = " + ithOccurrence);
		return find(s, pattern, ithOccurrence, 0);
	}

	private ISourceRange find(StringBuffer s, String pattern, int ithOccurrence, int startIdx) {
		if (startIdx < 0 || startIdx > s.length())
			throw new IllegalStateException("startIdx = " + startIdx);
		int idx = s.indexOf(pattern, startIdx);
		if (idx == -1)
			throw new IllegalStateException("not found \"" + pattern + "\" in \"" + s.substring(startIdx));
		if (ithOccurrence == 1)
			return new SourceRange(idx, pattern.length());
	    return find(s, pattern, ithOccurrence-1, idx+1);
	}
	
	public void testBreakFor() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      for (int i = 0; i < xs.length; i++) {\n");
		s.append("          break;");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("break");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "for", 1), find(s, "}", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testBreakForeach() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      for (int i : xs){\n");
		s.append("          break;");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("break");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "for", 1), find(s, "}", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testBreakWhile() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("  void foo(boolean b){\n");
		s.append("	    while (b) {\n");
		s.append("		   System.err.println(b);\n");
		s.append("		   break;\n");
		s.append("	    }\n");
		s.append("	}\n");
		s.append("}");
		int offset= 1 + s.indexOf("break");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "while", 1), find(s, "}", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testBreakDo() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("  void foo(boolean b){\n");
		s.append("	    do {\n");
		s.append("		   System.err.println(b);\n");
		s.append("		   break;\n");
		s.append("	    } while(b);\n");
		s.append("	}\n");
		s.append("}");
		int offset= 1 + s.indexOf("break");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "do", 1), find(s, "}", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testBreakSwitch() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("  void foo(int i){\n");
		s.append("    switch (i){\n");
		s.append("      case 1: System.err.println(i); break;\n");
		s.append("      default:System.out.println(i);\n"); 
		s.append("    }\n");
		s.append("  }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("break");//middle of word
		int length= 2;
		ISourceRange[] ranges= { find(s, "switch", 1), find(s, "}", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testLabeledBreakFor() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      bar: for (int i = 0; i < xs.length; i++) {\n");
		s.append("        do{\n");
		s.append("            break bar;");
		s.append("        }while (xs != null);\n");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("break");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "bar", 1), find(s, "}", 2)};
		checkSelection(s, offset, length, ranges);
	}
	
	public void testLabeledBreakFor1() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      bar: for (int i = 0; i < xs.length; i++) {\n");
		s.append("        baz: do{\n");
		s.append("            break bar;");
		s.append("        }while (xs != null);\n");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("break");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "bar", 1), find(s, "}", 2)};
		checkSelection(s, offset, length, ranges);
	}

	public void testContinueFor() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      for (int i = 0; i < xs.length; i++) {\n");
		s.append("          continue;");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("continue");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "for", 1)};
		checkSelection(s, offset, length, ranges);
	}
	
	public void testContinueForeach() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      for (int i : xs){\n");
		s.append("          continue;");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("continue");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "for", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testContinueWhile() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("  void foo(boolean b){\n");
		s.append("	    while (b) {\n");
		s.append("		   System.err.println(b);\n");
		s.append("		   continue;\n");
		s.append("	    }\n");
		s.append("	}\n");
		s.append("}");
		int offset= 1 + s.indexOf("continue");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "while", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testContinueDo() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("  void foo(boolean b){\n");
		s.append("	    do {\n");
		s.append("		   System.err.println(b);\n");
		s.append("		   continue;\n");
		s.append("	    } while(b);\n");
		s.append("	}\n");
		s.append("}");
		int offset= 1 + s.indexOf("continue");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "do", 1)};
		checkSelection(s, offset, length, ranges);
	}

	//continue skips over switches
	public void testContinueSwitch() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("  void foo(int i){\n");
		s.append("    do{\n");
		s.append("       switch (i){\n");
		s.append("         case 1: System.err.println(i); continue;\n");
		s.append("         default:System.out.println(i);\n"); 
		s.append("       }\n");
		s.append("    }while(i != 9);\n");
		s.append("  }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("continue");//middle of word
		int length= 2;
		ISourceRange[] ranges= { find(s, "do", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testLabeledContinueFor() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      bar: for (int i = 0; i < xs.length; i++) {\n");
		s.append("        do{\n");
		s.append("            continue bar;");
		s.append("        }while (xs != null);\n");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("continue");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "bar", 1)};
		checkSelection(s, offset, length, ranges);
	}
	
	public void testLabeledContinueFor1() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      bar: for (int i = 0; i < xs.length; i++) {\n");
		s.append("        baz: do{\n");
		s.append("            continue bar;");
		s.append("        }while (xs != null);\n");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("continue");//middle of word
		int length= 0;
		ISourceRange[] ranges= { find(s, "bar", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testLabeledContinueFor2() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   void foo(int[] xs){\n");
		s.append("      bar: for (int i = 0; i < xs.length; i++) {\n");
		s.append("        baz: do{\n");
		s.append("            continue bar;");
		s.append("        }while (xs != null);\n");
		s.append("      }\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= s.indexOf("continue bar;") + 1+ "continue ".length();//middle of label reference
		int length= 0;
		ISourceRange[] ranges= { find(s, "bar", 1)};
		checkSelection(s, offset, length, ranges);
	}
}
