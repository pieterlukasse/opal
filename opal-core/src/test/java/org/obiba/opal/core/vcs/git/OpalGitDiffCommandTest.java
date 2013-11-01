import java.util.Date;
  private static final String DIFF_VIEW_WITH_HEAD = "diff --git a/TestView/View.xml b/TestView/View.xml\n" +
      "index 8965792..9ba271d 100644\n" +
      "--- a/TestView/View.xml\n" +
      "+++ b/TestView/View.xml\n" +
      "@@ -14,8 +14,7 @@\n" +
      "           <attribute name=\"label\" valueType=\"text\" locale=\"en\">Place name</attribute>\n" +
      "           <attribute name=\"index\" valueType=\"text\">0</attribute>\n" +
      "           <attribute name=\"derivedFrom\" namespace=\"opal\" valueType=\"text\">/datasource/opal-data2/table/CA/variable/PLACE_NAME</attribute>\n" +
      "-          <attribute name=\"script\" valueType=\"text\">$(&apos;PLACE_NAME&apos;)\n" +
      "-$(&apos;PLACE_NAME&apos;)</attribute>\n" +
      "+          <attribute name=\"script\" valueType=\"text\">$(&apos;PLACE_NAME&apos;)</attribute>\n" +
      "         </attributes>\n" +
      "       </variable>\n" +
      "       <variable name=\"STATE\" valueType=\"text\" entityType=\"PostalCode\" unit=\"\" mimeType=\"\">\n" +
      "@@ -88,8 +87,13 @@\n" +
      "           <attribute name=\"script\" valueType=\"text\">$(&apos;COORDINATE&apos;)</attribute>\n" +
      "         </attributes>\n" +
      "       </variable>\n" +
      "+      <variable name=\"TOTO_VAR\" valueType=\"integer\" entityType=\"PostalCode\" unit=\"\" mimeType=\"\">\n" +
      "+        <attributes>\n" +
      "+          <attribute name=\"script\" valueType=\"text\">null</attribute>\n" +
      "+        </attributes>\n" +
      "+      </variable>\n" +
      "     </variables>\n" +
      "   </variables>\n" +
      "   <created valueType=\"datetime\">2013-09-17T16:09:00.773-0400</created>\n" +
      "-  <updated valueType=\"datetime\">2013-09-19T11:00:19.008-0400</updated>\n" +
      "+  <updated valueType=\"datetime\">2013-09-19T11:48:01.742-0400</updated>\n" +
      " </org.obiba.magma.views.View>\n" +
      "\\ No newline at end of file\n";

  public void testDiffWithValidCommit() {
  public void testDiffWithValidVariablePath() {
  public void testDiffWithInvalidCommit() {
  public void testDiffWithValidViewPath() {
  public void testDiffWithSelf() {
  public void testDiffWithTwoVersionsBack() {
  @Test
  public void testDiffWithCurrent() {
    try {
      OpalGitDiffCommand command = new OpalGitDiffCommand.Builder(vcs.getRepository(DATASOURCE_NAME), "HEAD")
          .addPath("TestView/View.xml").addDatasourceName(DATASOURCE_NAME).addPreviousCommitId(
              "be77432d15dec81b4c60ed858d5d678ceb247171").build();
      List<String> diffs = command.execute();
      assertThat(diffs, matches(DIFF_VIEW_WITH_HEAD));
    } catch(Exception e) {
      Assert.fail();
    }
  }

  @Test
  public void testDiffWithCurrentUsingGitVCS() {
    try {
      List<String> diffs = vcs.getDiffEntries(DATASOURCE_NAME, "HEAD", "be77432d15dec81b4c60ed858d5d678ceb247171", "TestView/View.xml");
      assertThat(diffs, matches(DIFF_VIEW_WITH_HEAD));
    } catch(Exception e) {
      Assert.fail();
    }
  }
