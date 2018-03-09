package com.i3

import groovy.json.JsonSlurper
import io.netty.handler.codec.http.HttpHeaders
import io.vertx.core.Vertx
import org.codehaus.groovy.eclipse.refactoring.PreferenceConstants
import org.codehaus.groovy.eclipse.refactoring.formatter.DefaultGroovyFormatter
import org.codehaus.groovy.eclipse.refactoring.formatter.FormatterPreferencesOnStore
import org.codehaus.groovy.eclipse.refactoring.formatter.IFormatterPreferences
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.TextSelection
import org.eclipse.text.edits.TextEdit

// https://mvnrepository.com/artifact/io.vertx/vertx-core
public class HttpServer {

    def VERTX_PORT = 9091
    def VERTX_IP = '127.0.0.1'

    public static void main(String[] args) {
        new HttpServer().run()
    }

    def run() {
        Vertx.vertx().createHttpServer().requestHandler { req ->
            processCall(req)
        }.listen(VERTX_PORT, VERTX_IP)

        println 'Service running at ' + VERTX_IP + ':' + VERTX_PORT
    }

    private processCall(req) {
        try {
            String contentType = req.request.headers().get(HttpHeaders.Names.CONTENT_TYPE);

            if (contentType?.startsWith('application/json')) {
                req.bodyHandler { body ->
                    def bodyString = body.toString()
                    def json = new JsonSlurper().parseText(bodyString)
                    req.response.end(format(json.groovyCode))
                }
                return
            }

            req.response.putHeader("content-type", "text/html; charset=utf-8")
            req.response.end(html)
        } catch (all) {
            req.response.with {
                statusCode = 404
                statusMessage = all
                end()
            }
        }
    }


    private getPath(req) {
        req.path()
    }

    private String format(String code) throws Exception {
        DefaultGroovyFormatter cf = initializeFormatter(code);
        IDocument dc = new Document(code.toString());
        TextEdit te = cf.format();
        te.apply(dc);
        dc.get()
    }

    private DefaultGroovyFormatter initializeFormatter(String code) {
        IPreferenceStore pref = null;
        IFormatterPreferences store = new FormatterPreferencesOnStore();

        //https://github.com/groovy/groovy-eclipse/blob/master/ide/org.codehaus.groovy.eclipse.refactoring/src/org/codehaus/groovy/eclipse/refactoring/PreferenceConstants.java
        store.useTabs = false
        store.tabSize = 4
        store.indentSize = 4
        store.indentationMultiline = PreferenceConstants.DEFAULT_INDENT_MULTILINE
        store.bracesStart = PreferenceConstants.SAME_LINE
        store.bracesEnd = PreferenceConstants.NEXT_LINE
        store.maxLineLength = PreferenceConstants.DEFAULT_MAX_LINE_LEN
        store.smartPaste = false
        store.indentEmptyLines = false
        store.removeUnnecessarySemicolons = true
        store.longListLength = PreferenceConstants.DEFAULT_LONG_LIST_LENGTH

        IDocument doc = new Document(code.toString());
        TextSelection sel = new TextSelection(0, code.length());
        DefaultGroovyFormatter formatter = new DefaultGroovyFormatter(sel, doc, store, false);
        return formatter;
    }


    def html = """
<html>
    <body>
        <form id="form" action="" method="post">
            Groovy Code:<br>
            <textarea id="groovyCode" rows="20" cols="80" name="groovyCode"></textarea>
            <input id="submit" type="button" value="Submit">
        </form>

        <textarea id="response" rows="20" cols="80" name="response"></textarea>

        <script src="https://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js"></script>
        <script>
            \$(document).ready(function(){
                // click on button submit
                \$("#submit").on('click', function(){
                    var data = {}
                    data.groovyCode = \$('#groovyCode').val()
                    
                    var json = JSON.stringify(data)
                    
                    console.log(json)
                    
                    // send ajax
                    \$.ajax({
                        url: '/', // url where to submit the request
                        type : "POST", // type of action POST || GET
                        contentType:"application/json; charset=utf-8",
                        datatype : 'json', // data type
                        data : json, // post data || get data
                        success : function(result) {
                            // you can see the result from the console
                            // tab of the developer tools
                            console.log(result);
                            \$('#response').val(result)
                        },
                        error: function(xhr, resp, text) {
                            console.log(xhr, resp, text);
                        }
                    })
                });
            });
        </script>
    </body>
</html>
"""

}

/*

curl -H "Content-Type: application/json" -X POST -d '
{
"groovyCode":"import com.i3.settings.*

class ValueBuddyO{

   def enableResoucePacking = false;;

 def aaa(ddd){

if(ddd){
ddd =123
}else{
ddd =345
}
}
"
}
' http://0.0.0.0:8080/

 */