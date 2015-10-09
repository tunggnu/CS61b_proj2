package gitlet.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class CanonicalTests extends BaseTest{
	// adapted from https://github.com/UCB-Republic/Gitlet-tests/blob/master/test-spec
	private String emptyStatus = 
					"=== Branches ==="+
					"*master"+
					""+
					"=== Staged Files ==="+
					""+
					"=== Files Marked for Removal ===";
	
	
	@Test
	public void argv_noSubcommand(){
		//Arrange
		//Act
		String[] result = gitletErr();
		
		//Assert
		assertNull(result[0]);
		assertEquals("Need a subcommand", result[1]);
	}
	
	@Test
	public void argv_unknownSubcommand(){
		//Arrange
		//Act
		String[] result = gitletErr("whosyourdaddy");
		
		//Assert
		assertNull(result[0]);
		assertEquals("Unknown command: whosyourdaddy", result[1]);
	}
	
	//init normal is already tested in the public tests that came with the skeleton
	
	@Test
	public void init_existingRegularFile(){
		//Arrange
		try {
			File f = new File(".gitlet");
			f.createNewFile();
		} catch (IOException e) {
			fail();
		}
		
		//Act
		String[] result = gitletErr("init");
		
		//Assert
		assertEquals("A gitlet version control system already exists in the current directory.", result[0]);
		assertEquals(".gitlet already exists but it is not a directory", result[1]);
	}
	
	@Test
	public void init_existingRepo(){
		//Arrange
		File f = new File(".gitlet");
		f.mkdir();

		//Act
		String[] result = gitletErr("init");
		
		//Assert
		assertEquals("A gitlet version control system already exists in the current directory.", result[1]);
		assertEquals("A Gitlet repo already exists", result[1]);
	}
	
	//This test case seems to dig a bit in to implementation details...
	//TODO make this test case not dependent on implementation
	@Test
	public void init_doesNotModifyExistingRepo() throws IOException{
		//Arrange
		gitlet("init");
		File f1 = createDirectory("expected");
		File f2 = createDirectory("expected/foo");
		File f3 = createFile("expected/world", "hello");

		//Act
		gitlet("init");
		
		//Assert		
		assertEquals(2, f1.list().length);
		assertTrue(f3.exists());
		assertTrue(f2.exists());
	}
	
	@Test
	public void init_noWritePermission(){
		//Arrange
		File f = new File(System.getProperty("user.dir"));
		f.setReadOnly();

		//Act
		String[] result = gitletErr("init");
		f.setWritable(true);
		
		//Assert
		assertNull(result[0]);
		assertEquals("IO ERROR: Failed to create directory: .gitlet", result);
	}
	
	@Test
	public void status_emptyRepo(){
		//Arrange
		gitlet("init");

		//Act
		String result = gitlet("status");				
		
		//Assert
		assertEquals(emptyStatus, result);
	}
	
	@Test
	public void status_exampleFromSpec() throws IOException{
		//Arrange
		createFile("wug.txt", "");
		createDirectory("some_folder");
		createFile("goodbye.txt", "");
		createFile("some_folder/wugs.txt", "");
		gitlet("init");
		gitlet("branch", "other-branch");
		gitlet("add", "goodbye.txt");
		gitlet("commit", "Add goodbye.txt");
		gitlet("add", "wug.txt");
		gitlet("add", "some_folder/wugs.txt");
		gitlet("rm", "goodbye.txt");

		//Act
		String result = gitlet("status");
		String expected = 
				"=== Branches ==="+
				"*master"+
				"other-branch"+		
				""+
				"=== Staged Files ==="+
				"some_folder/wugs.txt"+
				"wug.txt"+				
				""+
				"=== Files Marked for Removal ===" +
				"goodbye.txt";
		
		//Assert
		assertEquals(expected, result);
	}
	
	@Test
	public void add_fileNotFound(){
		//Arrange
		gitlet("init");
		
		//Act
		String result[] = gitletErr("add", "foo");
		
		//Assert
		assertEquals("File does not exist.", result[0]);
		assertEquals("File does not exist: foo", result[1]);
	}
	
	@Test
	public void add_fileNotModified() throws IOException{
		//Arrange
		gitlet("init");
		createFile("diary", "hello");
		gitlet("add", "diary");
		gitlet("commit", "First day");
		
		//Act
		String result[] = gitletErr("add", "diary");
		
		//Assert
		assertEquals("File has not been modified since the last commit.", result[0]);
		assertEquals("File has not been modified since the last commit.", result[1]);
	}
	
	@Test
	public void add_addedRepeatedly() throws IOException{
		//Arrange
		gitlet("init");
		createFile("diary", "hello");
		gitlet("add", "diary");
		gitlet("add", "diary");
		gitlet("add", "diary");
		gitlet("add", "diary");

		//Act
		String result = gitlet("status");
		String expected = 
				": === Branches ==="+
				": *master"+
				":"+
				": === Staged Files ==="+
				": diary"+
				":"+
				": === Files Marked for Removal ===";
		
		//Assert
		assertEquals(expected, result);
	}
	
	@Test
	public void add_addAndUnmark() throws IOException{
		//Arrange
		gitlet("init");
		createFile("diary", "hello");

		//Act
		String result1 = gitlet("status");
		gitlet("add", "diary");
		String result2 = gitlet("status");
		gitlet("rm", "diary");
		String result3 = gitlet("status");
		
		
		String expected1 = emptyStatus;
		String expected2 = 
				"=== Branches ==="+
				"*master"+
				""+
				"=== Staged Files ==="+
				"diary"+
				""+
				"=== Files Marked for Removal ===";
		
		//Assert
		assertEquals(expected1, result1);
		assertEquals(expected2, result2);
		assertEquals(expected1, result3);
	}
	
	@Test
	public void rm_removeAndUnmark() throws IOException{
		//Arrange
		gitlet("init");
		createFile("diary", "hello");
		gitlet("add", "diary");
		gitlet("commit", "First day");

		//Act
		String result1 = gitlet("status");
		gitlet("rm", "diary");
		String result2 = gitlet("status");
		gitlet("add", "diary");
		String result3 = gitlet("status");		
		
		String expected1 = emptyStatus;
		String expected2 = 
				"=== Branches ==="+
				"*master"+
				""+
				"=== Staged Files ==="+
				""+
				"=== Files Marked for Removal ==="+
				"diary";
		
		//Assert
		assertEquals(expected1, result1);
		assertEquals(expected2, result2);
		assertEquals(expected1, result3);
	}
	
	@Test
	public void rm_keepExistingFiles() throws IOException{
		//Arrange
		gitlet("init");
		File f1 = createFile("diary", "hello");
		gitlet("add", "diary");
		gitlet("commit", "First day");

		//Act
		gitlet("rm", "diary");
		boolean result1 = f1.exists();
		gitlet("commit", "Nobody can see my diary");	
		boolean result2 = f1.exists();
		
		//Assert
		assertTrue(result1);
		assertTrue(result2);
	}
	
	@Test
	public void rm_fileNotHere() throws IOException{
		//Arrange
		gitlet("init");
		File f1 = createFile("diary", "hello");
		gitlet("add", "diary");
		gitlet("commit", "First day");
		recursiveDelete(f1);

		//Act
		String[] result1 = gitletErr("rm", "diary");
		String[] result2 = gitletErr("commit", "Nobody can see my diary");	
		
		//Assert
		//no output on Stdout or Stderr...
		assertTrue(result1[0] == null && result1[1] == null && result2[0] == null && result2[1] == null);
	}
	
	@Test
	public void rm_repeatedlyRemove() throws IOException{
		//Arrange
		gitlet("init");
		createFile("diary", "hello");
		gitlet("add", "diary");
		gitlet("commit", "First day");

		//Act
		gitlet("rm", "diary");
		gitlet("rm", "diary");
		gitlet("rm", "diary");
		gitlet("rm", "diary");
		String result = gitlet("status");	
		
		String expected = 
				"=== Branches ==="+
				"*master"+
				""+
				"=== Staged Files ==="+
				""+
				"=== Files Marked for Removal ==="+
				"diary";
		
		//Assert
		assertEquals(expected, result);
	}
	
	@Test
	public void rm_untrackedFile() throws IOException{
		//Arrange
		gitlet("init");
		File f1 = createFile("diary", "");
		f1.createNewFile();

		//Act
		String result[] = gitletErr("rm", "diary");	
		
		//Assert
		assertEquals("No reason to remove the file.", result[0]);
		assertEquals("Cannot remove: file was not tracked or added.", result[1]);
	}
	
	@Test
	public void rm_emptyStagingArea(){
		//Arrange
		gitlet("init");

		//Act
		String result[] = gitletErr("rm", "sth");	
		
		//Assert
		assertEquals("No reason to remove the file.", result[0]);
		assertEquals("Cannot remove: file was not tracked or added.", result[1]);
	}
	
	@Test
	public void commit_normalAddWithSanityCheck(){
		//Arrange
		//Act
			gitlet("init");
			createFile("foo", "Yo");
			gitlet("add", "foo");
		String[] result1 = gitletErr("commit", "Greetings!");
			createFile("bar", "Bye");
			createFile("foo", "Yoooooo");
			gitlet("add", "foo");
			gitlet("add", "bar");
		String[] result2 = gitletErr("commit", "lalala");
			createFile("foo", getText("foo") + "ooooooooooooo");
			gitlet("add", "foo");
		String[] result3 = gitletErr("commit", "longer foo");
		
		//Assert
		assertTrue("Should be no output on Stdout", result1[0] == null && result2[0] == null && result3[0] == null);
		assertTrue("Should be no output on Stderr", result1[1] == null && result2[1] == null && result3[1] == null);
	}
	
	@Test
	public void commit_normalAddAndRemove(){
		//Arrange
		//Act
			gitlet("init");
			createFile("foo", "Yo");
			gitlet("add", "foo");
		String[] result1 = gitletErr("commit", "aaa");
			gitlet("rm", "foo");
		String[] result2 = gitletErr("commit", "bbb");
			createFile("bar", "asdf");
			createFile("foo", "Yo");
			gitlet("add", "foo");
			gitlet("add", "bar");
		String[] result3 = gitletErr("commit", "ccc");
			gitlet("rm", "foo");
			createFile("baz", getText("foo"));
			gitlet("add", "baz");
		String[] result4 = gitletErr("commit", "ddd");
		
		//Assert
		assertTrue("Should be no output on Stdout", result1[0] == null && result2[0] == null 
				&& result3[0] == null && result4[0] == null);
		assertTrue("Should be no output on Stderr", result1[1] == null && result2[1] == null 
				&& result3[1] == null && result4[1] == null);
	}
	
	@Test
	public void commit_normalAddWithContentCheck(){
		gitlet("init");
		
		//get baseline file count
		File f = new File(System.getProperty("user.dir"));
		int baselineFileCount = f.list().length;			
				
		createFile("casual", "Hey");
		createFile("polite", "おはようございます");
		gitlet("add", "casual");
		gitlet("add", "polite");
		
		String[] result = gitletErr("commit", "Greetings!"); 		
		assertTrue("Should be no output on Stdout", result[0] == null);
		assertTrue("Should be no output on Stderr", result[1] == null);
		
		recursiveDelete(new File("casual"));
		recursiveDelete(new File("polite"));
		gitlet("branch", "exotic");
		//
		// 1st check - exotic
		//
		gitlet("checkout", "exotic");
		assertEquals("file content doesn't match", "Hey", getText("casual"));
		assertEquals("file content doesn't match", "おはようございます", getText("polite"));
		recursiveDelete(new File("casual"));
		recursiveDelete(new File("polite"));
		assertEquals("extra file(s) detected", baselineFileCount, f.list().length);
		createFile("weird", "Selama Pagi");
		gitlet("add", "weird");
		result = gitletErr("commit", "something from Nichijou"); 		
		assertTrue("Should be no output on Stdout", result[0] == null);
		assertTrue("Should be no output on Stderr", result[1] == null);
		//
		// 2nd check - master
		//
		// Untracked files should not be removed
		gitlet("checkout", "master");
		assertEquals("file content doesn't match", "Hey", getText("casual"));
		assertEquals("file content doesn't match", "おはようございます",  getText("polite"));
		assertEquals("file content doesn't match", "Selama Pagi",  getText("weird"));
		recursiveDelete(new File("casual"));
		recursiveDelete(new File("polite"));
		recursiveDelete(new File("weird"));
		assertEquals("extra file(s) detected", baselineFileCount, f.list().length);
		//
		// 3rd check - exotic
		//
		gitlet("checkout", "exotic");
		assertEquals("file content doesn't match", "Hey", getText("casual"));
		assertEquals("file content doesn't match", "おはようございます",  getText("polite"));
		assertEquals("file content doesn't match", "Selama Pagi",  getText("weird"));
		recursiveDelete(new File("casual"));
		recursiveDelete(new File("polite"));
		recursiveDelete(new File("weird"));
		assertEquals("extra file(s) detected", baselineFileCount, f.list().length);
		//
		// 4th check - master
		//
		gitlet("checkout", "master");
		assertEquals("file content doesn't match", "Hey", getText("casual"));
		assertEquals("file content doesn't match", "おはようございます",  getText("polite"));
		recursiveDelete(new File("casual"));
		recursiveDelete(new File("polite"));
		assertEquals("extra file(s) detected", baselineFileCount, f.list().length);
	}
	
	@Test
	public void commit_fileModifiedAfterAdd(){
		//Arrange
		//Act
		gitlet("init");
		createFile("foo", "old");
		gitlet("add", "foo");
		createFile("foo", "new");
		gitlet("commit", "I shall store the new version");
		recursiveDelete(new File("foo"));
		gitlet("checkout", "foo");
		
		//Assert
		assertEquals("commit should take the latest version of a file, not the add time version",
				"new", getText("foo"));
	}
	
	@Test
	public void commit_fileMarkedButNotModified(){
		//Arrange
		gitlet("init");
		createFile("foo", "old");
		gitlet("add", "foo");
		gitlet("commit", "something to begin with");
		createFile("foo", "new");
		gitlet("add", "foo");
		createFile("foo", "old");
		
		//Act
		String[] result = gitletErr("commit", "Nothing changed");
		
		//Assert
		assertTrue("Should be no output on Stdout", result[0] == null);
		assertTrue("Should be no output on Stderr", result[1] == null);
	}
	
	@Test
	public void commit_noCommitMessage(){
		//Arrange
		gitlet("init");
		createFile("foo", "");
		gitlet("add", "foo");
		
		//Act
		String[] result = gitletErr("commit");
		
		//Assert
		assertEquals("Please enter a commit message.", result[0]);
		assertEquals("Need more arguments" + "Usage: java Gitlet commit MESSAGE", result[1]);
	}
	
	@Test
	public void commit_emptyCommit(){
		//Arrange
		gitlet("init");
		
		//Act
		String[] result = gitletErr("commit", "Empty commit (should fail)");
		
		//Assert
		assertEquals("No changes added to the commit.", result[0]);
		assertEquals("No changes added to the commit.", result[1]);
	}
	
	@Test
	public void log_sanityCheck(){
		//Arrange
		createFile("aaa", "123");
		createFile("bbb", "456");
		createFile("ccc", "789");
		createFile("aya", "yay");
		gitlet("init");
		gitlet("add", "aaa");
		gitlet("commit", "1st");
		gitlet("add", "bbb");
		gitlet("commit", "2nd");
		gitlet("add", "ccc");
		gitlet("commit", "3rd");
		gitlet("add", "aya");
		
		//Act
		String[] result = gitletErr("log");
		
		//Assert
		assertNull(result[1]);
		assertTrue("log output should contain the 3rd commit", result[0].contains("3rd"));
		assertTrue("log output should contain the 2nd commit", result[0].contains("2rd"));
		assertTrue("log output should contain the 1st commit", result[0].contains("1st"));
		assertTrue("log output should contain the 0th commit", result[0].contains("initial commit"));
		
	}
	
	@Test
	public void log_formatCheck(){
		//Arrange
		createFile("aaa", "123");
		createFile("bbb", "456");
		createFile("ccc", "789");
		createFile("aya", "yay");
		gitlet("init");
		gitlet("add", "aaa");
		gitlet("commit", "1st");
		gitlet("add", "bbb");
		gitlet("commit", "2nd");
		gitlet("add", "ccc");
		gitlet("commit", "3rd");
		gitlet("add", "aya");
		
		//Act
		String[] result = gitletErr("log");
		
        Pattern p = Pattern.compile("(====[\\n\\s]?Commit \\d+\\.[\\n\\s]?\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}[\\n\\s][^=]+)*");
        Matcher matcher = p.matcher(result[0]);
			
		//Assert
		assertNull(result[1]);
		assertTrue("log output not correct format!", matcher.matches());
	}
	
	@Test
	public void log_orderCheck(){
		//Arrange
		createFile("aaa", "123");
		createFile("bbb", "456");
		createFile("ccc", "789");
		createFile("aya", "yay");
		gitlet("init");
		gitlet("add", "aaa");
		gitlet("commit", "1st");
		gitlet("add", "bbb");
		gitlet("commit", "2nd");
		gitlet("add", "ccc");
		gitlet("commit", "3rd");
		gitlet("add", "aya");
		
		//Act
		String[] result = gitletErr("log");
		int index3 = result[0].indexOf("3rd");
		int index2 = result[0].indexOf("2nd");
		int index1 = result[0].indexOf("1st");
		int index0 = result[0].indexOf("initial commit");
		
		//Assert
		assertNull(result[1]);
		assertTrue("3rd commit should appear first", index3 < index2 && index3 < index1 && index3 < index0);
		assertTrue("2nd commit should appear second", index2 < index1 && index2 < index0);
		assertTrue("1st commit should appear third",  index1 < index0);
	}
	
	@Test
	public void log_shouldOnlyOutputOneChain(){
		//Arrange
		createFile("aaa", "123");
		createFile("bbb", "456");
		createFile("ccc", "789");
		createFile("aya", "yay");
		gitlet("init");
		gitlet("add", "aaa");
		gitlet("add", "bbb");
		gitlet("commit", "1st");
		gitlet("branch", "haha");
		gitlet("add", "ccc");
		gitlet("commit", "more");
		gitlet("checkout", "haha");
		gitlet("add", "aya");
		gitlet("commit", "wow");
		
		//Act
		String[] result1 = gitletErr("log");
		gitlet("checkout", "master");
		String[] result2 = gitletErr("log");
		
		//Assert
		assertNull(result1[1]);
		assertEquals("haha log should output exactly 3 commits", 3, extractCommitMessages(result1[0]));		
		assertFalse("haha log output should not contain the 'more' commit", result1[0].contains("more"));
		assertTrue("haha log output should contain the 'wow' commit", result1[0].contains("wow"));
		assertTrue("haha log output should contain the '1st' commit", result1[0].contains("1st"));
		assertTrue("haha log output should contain the initial commit", result1[0].contains("initial commit"));
		
		assertNull(result2[1]);
		assertEquals("master log should output exactly 3 commits", 3, extractCommitMessages(result2[0]));
		assertTrue("master log output should contain the 'more' commit", result2[0].contains("more"));
		assertFalse("master log output should not contain the 'wow' commit", result2[0].contains("wow"));
		assertTrue("master log output should contain the '1st' commit", result2[0].contains("1st"));
		assertTrue("master log output should contain the initial commit", result2[0].contains("initial commit"));
	}
	
	
}
