/*
 * Create multiple pages in Confluence space, with a given hierarchical structure.
 * 
 * Inspired by: https://library.adaptavist.com/entity/project-creator-for-confluence
 * But with less reliance on the Atlassian classes and more pure Java/Groovy scripting.
 * 
 * To use this code, install ScriptRunner in your Confluence Space: https://www.scriptrunnerhq.com/atlassian-apps/confluence/scriptrunner-for-confluence
 * Then, in the 'Script Console' page, copy+paste this script.
 * Then adapt the page hierarchy structure to fit your needs.
 * Then execute.
 */

// Define global variables
def spaceKey = "YourSpaceName"
def parentPage = "ParentPageID"
def templateKey = "YourTemplateName"

/*
 * Create a new page.
 *
 * @param spaceKey The space that the page should be created in.
 * @param parentKey The key of the parent page.
 * @param pageName The name of the new page.
 * @param templateName The name of the template to be used.
 */
String createPage(String spaceKey, String parentKey, String pageName, String templateName) {
    def templates = get("/wiki/rest/api/template/page?spaceKey=${spaceKey}").asObject(Map).body.results as List<Map<String, Object>>
    def template = templates.find { it.name == templateName }
    def templateResult = get("/wiki/rest/api/template/${template.templateId}").asObject(Map).body
    def templateBody = templateResult.body
    def result = post('/wiki/rest/api/content')
        .header('Content-Type', 'application/json')
        .body([
            space: [key: spaceKey],
            status: "current",
            title: pageName,
            type: "page",
            ancestors: [[id: parentKey]],
            body: templateBody
        ])
        .asObject(Map).body
    if (!result.statusCode) {
        logger.info("Created a page: '{}' from template: '{}'", result.id, templateName)
    } else {
        logger.error("Failure with error code: '{}'", result.statusCode)
    }
    result.id
}

/*
 * Create a series of sub-pages, given a specific hierarchical structure.
 *
 * @param spaceKey The space that the page should be created in.
 * @param parentKey The key of the parent page.
 * @param subPages The hierarchical structure of pages to create. Should be a `List` of `Object`s. Each objects must have the `pageName` key, and an optional `subPages` key.
 * @param templateName The name of the template to be used.
 */
void createPages(String spaceKey, String parentKey, List subPages, String templateName){
    subPages.forEach { pageSettings ->
        def pageId = createPage(spaceKey, parentKey, pageSettings.pageName as String, templateName)
        if (pageSettings.subPages) {
            createPages(spaceKey, pageId, pageSettings.subPages, templateName)
        }
    }
}

// Build page hierarchy
def subPages = [
    [pageName: "Sub Page 1"],
    [
        pageName: "Sub Page 2, with Children",
        subPages: [
            [pageName: "Sub sub page 2"]
        ]
    ],
    [pageName: "Sub page 3"],
]

// Execute
createPages(spaceKey, parentPage, subPages, templateKey)
