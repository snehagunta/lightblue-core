package com.redhat.lightblue.eval;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.query.UpdateExpression;
import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.test.AbstractJsonNodeTest;

public class ForEachExpressionEvaluatorTest extends AbstractJsonNodeTest {
    private EntityMetadata md;

    @Before
    public void setUp() throws Exception {
        doc = EvalTestContext.getDoc("./sample1.json");
        md = EvalTestContext.getMd("./testMetadata.json");
    }

    @Test
    public void array_foreach_removeall() throws Exception {
        UpdateExpression expr = EvalTestContext.updateExpressionFromJson("{ '$foreach' : { 'field7' : '$all', '$update' : '$remove' } }");

        Updater updater = Updater.getInstance(JSON_NODE_FACTORY, md, expr);
        Assert.assertTrue(updater.update(doc, md.getFieldTreeRoot(), new Path()));

        Assert.assertEquals(0, doc.get(new Path("field7")).size());
        Assert.assertEquals(0, doc.get(new Path("field7#")).asInt());
    }

    @Test
    public void array_foreach_removeone() throws Exception {
        UpdateExpression expr = EvalTestContext.updateExpressionFromJson("{ '$foreach' : { 'field7' : { 'field':'elemf1','op':'=','rvalue':'elvalue0_1'} , '$update' : '$remove' } }");
        Updater updater = Updater.getInstance(JSON_NODE_FACTORY, md, expr);
        Assert.assertTrue(updater.update(doc, md.getFieldTreeRoot(), new Path()));

        Assert.assertEquals(3, doc.get(new Path("field7")).size());
        Assert.assertEquals("elvalue1_1", doc.get(new Path("field7.0.elemf1")).asText());
        Assert.assertEquals(3, doc.get(new Path("field7#")).asInt());
        Assert.assertEquals(3, doc.get(new Path("field7")).size());
    }

    @Test
    public void array_foreach_modone() throws Exception {
        UpdateExpression expr = EvalTestContext.updateExpressionFromJson("{ '$foreach' : { 'field7' : { 'field':'elemf1','op':'=','rvalue':'elvalue0_1'} , '$update' : {'$set': { 'elemf1':'test'}} } }");
        Updater updater = Updater.getInstance(JSON_NODE_FACTORY, md, expr);
        Assert.assertTrue(updater.update(doc, md.getFieldTreeRoot(), new Path()));

        Assert.assertEquals(4, doc.get(new Path("field7")).size());
        Assert.assertEquals("test", doc.get(new Path("field7.0.elemf1")).asText());
    }

    @Test
    public void one_$parent_array_foreach_modone() throws Exception {
        UpdateExpression expr = EvalTestContext.updateExpressionFromJson("{ '$foreach' : { 'field6.$parent.field7' : { 'field':'elemf1','op':'=','rvalue':'elvalue0_1'} , '$update' : {'$set': { 'elemf1':'test'}} } }");
        Updater updater = Updater.getInstance(JSON_NODE_FACTORY, md, expr);
        Assert.assertTrue(updater.update(doc, md.getFieldTreeRoot(), new Path()));

        Assert.assertEquals(4, doc.get(new Path("field7")).size());
        Assert.assertEquals("test", doc.get(new Path("field7.0.elemf1")).asText());
    }

    @Test
    public void one_$parent_array_foreach_removeone() throws Exception {
        UpdateExpression expr = EvalTestContext.updateExpressionFromJson("{ '$foreach' : { 'field6.$parent.field7' : { 'field':'elemf1','op':'=','rvalue':'elvalue0_1'} , '$update' : '$remove' } }");
        Updater updater = Updater.getInstance(JSON_NODE_FACTORY, md, expr);
        Assert.assertTrue(updater.update(doc, md.getFieldTreeRoot(), new Path()));

        Assert.assertEquals(3, doc.get(new Path("field7")).size());
        Assert.assertEquals("elvalue1_1", doc.get(new Path("field7.0.elemf1")).asText());
        Assert.assertEquals(3, doc.get(new Path("field7#")).asInt());
        Assert.assertEquals(3, doc.get(new Path("field7")).size());
    }

    @Test
    public void one_$parent_array_foreach_removeall() throws Exception {
        UpdateExpression expr = EvalTestContext.updateExpressionFromJson("{ '$foreach' : { 'field6.$parent.field7' : '$all', '$update' : '$remove' } }");

        Updater updater = Updater.getInstance(JSON_NODE_FACTORY, md, expr);
        Assert.assertTrue(updater.update(doc, md.getFieldTreeRoot(), new Path()));

        Assert.assertEquals(0, doc.get(new Path("field7")).size());
        Assert.assertEquals(0, doc.get(new Path("field7#")).asInt());
    }
}
