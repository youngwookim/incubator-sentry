/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sentry.hdfs;

import java.util.List;

import org.apache.hadoop.fs.Path;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestHMSPaths {

  @Test
  public void testGetPathElements() {
    List<String> as2 = HMSPaths.getPathElements(new String("/a/b"));
    List<String> as1 = HMSPaths.getPathElements(new String("/a/b"));
    Assert.assertEquals(as1, as2);

    List<String> as = HMSPaths.getPathElements(new String("/a/b"));
    Assert.assertEquals(Lists.newArrayList("a", "b"), as);

    as = HMSPaths.getPathElements(new String("//a/b"));
    Assert.assertEquals(Lists.newArrayList("a", "b"), as);

    as = HMSPaths.getPathElements(new String("/a//b"));
    Assert.assertEquals(Lists.newArrayList("a", "b"), as);

    as = HMSPaths.getPathElements(new String("/a/b/"));
    Assert.assertEquals(Lists.newArrayList("a", "b"), as);

    as = HMSPaths.getPathElements(new String("//a//b//"));
    Assert.assertEquals(Lists.newArrayList("a", "b"), as);
  }

  @Test
  public void testEntryType() {
    Assert.assertTrue(HMSPaths.EntryType.DIR.isRemoveIfDangling());
    Assert.assertFalse(HMSPaths.EntryType.PREFIX.isRemoveIfDangling());
    Assert.assertFalse(
        HMSPaths.EntryType.AUTHZ_OBJECT.isRemoveIfDangling());
  }
  
  @Test
  public void testRootEntry() {
    HMSPaths.Entry root = HMSPaths.Entry.createRoot(false);
    root.toString();
    Assert.assertNull(root.getParent());
    Assert.assertEquals(HMSPaths.EntryType.DIR, root.getType());
    Assert.assertNull(root.getAuthzObj());
    Assert.assertEquals(Path.SEPARATOR, root.getFullPath());
    Assert.assertTrue(root.getChildren().isEmpty());
    root.delete();
    try {
      root.find(null, true);
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }
    try {
      root.find(new String[0], true);
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }
    try {
      root.find(null, false);
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }
    try {
      root.find(new String[0], false);
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }
    Assert.assertNull(root.find(new String[]{"a"}, true));
    Assert.assertNull(root.find(new String[]{"a"}, false));
    Assert.assertNull(root.findPrefixEntry(Lists.newArrayList("a")));

    root.delete();
  }

  @Test
  public void testRootPrefixEntry() {
    HMSPaths.Entry root = HMSPaths.Entry.createRoot(true);
    root.toString();

    Assert.assertNull(root.find(new String[]{"a"}, true));
    Assert.assertNull(root.find(new String[]{"a"}, false));
    Assert.assertEquals(root, root.findPrefixEntry(Lists.newArrayList("a")));
    Assert.assertEquals(root, root.findPrefixEntry(Lists.newArrayList("a", "b")));

    try {
      root.createPrefix(Lists.newArrayList("a"));
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }
  }

  @Test
  public void testImmediatePrefixEntry() {
    HMSPaths.Entry root = HMSPaths.Entry.createRoot(false);
    HMSPaths.Entry entry = root.createPrefix(Lists.newArrayList("a"));
    entry.toString();
    
    Assert.assertEquals(1, root.getChildren().size());

    Assert.assertEquals(root, entry.getParent());
    Assert.assertEquals(HMSPaths.EntryType.PREFIX, entry.getType());
    Assert.assertEquals("a", entry.getPathElement());
    Assert.assertNull(entry.getAuthzObj());
    Assert.assertEquals(Path.SEPARATOR + "a", entry.getFullPath());
    Assert.assertTrue(entry.getChildren().isEmpty());

    Assert.assertEquals(entry, root.findPrefixEntry(Lists.newArrayList("a")));
    Assert.assertEquals(entry, root.findPrefixEntry(Lists.newArrayList("a", "b")));

    Assert.assertNull(root.find(new String[]{"a", "b"}, false));

    Assert.assertNull(root.find(new String[]{"b"}, false));
    Assert.assertNull(root.findPrefixEntry(Lists.newArrayList("b")));

    try {
      root.createPrefix(Lists.newArrayList("a", "b"));
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }

    try {
      root.createPrefix(Lists.newArrayList("a", "b", "c"));
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }

    entry.delete();
    Assert.assertTrue(root.getChildren().isEmpty());
  }

  @Test
  public void testFurtherPrefixEntry() {
    HMSPaths.Entry root = HMSPaths.Entry.createRoot(false);
    HMSPaths.Entry entry = root.createPrefix(Lists.newArrayList("a", "b"));
    entry.toString();

    Assert.assertEquals(1, root.getChildren().size());

    Assert.assertEquals(root, entry.getParent().getParent());
    Assert.assertEquals(HMSPaths.EntryType.PREFIX, entry.getType());
    Assert.assertEquals(HMSPaths.EntryType.DIR, 
        entry.getParent().getType());
    Assert.assertEquals("b", entry.getPathElement());
    Assert.assertEquals("a", entry.getParent().getPathElement());
    Assert.assertNull(entry.getAuthzObj());
    Assert.assertNull(entry.getParent().getAuthzObj());
    Assert.assertEquals(Path.SEPARATOR + "a" + Path.SEPARATOR + "b", 
        entry.getFullPath());
    Assert.assertEquals(Path.SEPARATOR + "a", entry.getParent().getFullPath());
    Assert.assertTrue(entry.getChildren().isEmpty());
    Assert.assertEquals(1, entry.getParent().getChildren().size());

    Assert.assertEquals(entry, root.findPrefixEntry(Lists.newArrayList("a", "b")));
    Assert.assertNull(root.findPrefixEntry(Lists.newArrayList("a")));

    Assert.assertNull(root.find(new String[]{"a", "b", "c"}, false));

    try {
      root.createPrefix(Lists.newArrayList("a", "b"));
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }

    try {
      root.createPrefix(Lists.newArrayList("a", "b", "c"));
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }

    entry.delete();
    Assert.assertTrue(root.getChildren().isEmpty());
  }

  @Test
  public void testImmediateAuthzEntry() {
    HMSPaths.Entry root = HMSPaths.Entry.createRoot(false);
    HMSPaths.Entry prefix = root.createPrefix(Lists.newArrayList("a", "b"));

    HMSPaths.Entry entry = root.createAuthzObjPath(
        Lists.newArrayList("a", "b", "p1"), "A");
    Assert.assertEquals(prefix, entry.getParent());
    Assert.assertEquals(HMSPaths.EntryType.AUTHZ_OBJECT, entry.getType());
    Assert.assertEquals("p1", entry.getPathElement());
    Assert.assertEquals("A", entry.getAuthzObj());
    Assert.assertEquals(Path.SEPARATOR + "a" + Path.SEPARATOR + "b" +
        Path.SEPARATOR + "p1", entry.getFullPath());

    try {
      root.createPrefix(Lists.newArrayList("a", "b", "p1", "c"));
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }

    Assert.assertEquals(entry, root.find(new String[]{"a", "b", "p1"}, true));
    Assert.assertEquals(entry, root.find(new String[]{"a", "b", "p1"}, false));
    Assert.assertEquals(entry, root.find(new String[]{"a", "b", "p1", "c"}, 
        true));
    Assert.assertNull(root.find(new String[]{"a", "b", "p1", "c"}, false));
    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "p1")));

    root.find(new String[]{"a", "b", "p1"}, true).delete();
    Assert.assertNull(root.find(new String[]{"a", "b", "p1"}, false));
    Assert.assertNull(root.find(new String[]{"a", "b"}, false));
    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "p1")));

  }

  @Test
  public void testFurtherAuthzEntry() {
    HMSPaths.Entry root = HMSPaths.Entry.createRoot(false);
    HMSPaths.Entry prefix = root.createPrefix(Lists.newArrayList("a", "b"));

    HMSPaths.Entry entry = root.createAuthzObjPath(
        Lists.newArrayList("a", "b", "t", "p1"), "A");
    Assert.assertEquals(prefix, entry.getParent().getParent());
    Assert.assertEquals(HMSPaths.EntryType.AUTHZ_OBJECT, entry.getType());
    Assert.assertEquals("p1", entry.getPathElement());
    Assert.assertEquals("A", entry.getAuthzObj());
    Assert.assertEquals(Path.SEPARATOR + "a" + Path.SEPARATOR + "b" +
        Path.SEPARATOR + "t" + Path.SEPARATOR + "p1", entry.getFullPath());

    try {
      root.createPrefix(Lists.newArrayList("a", "b", "p1", "t", "c"));
      Assert.fail();
    } catch (IllegalArgumentException ex) {
      //NOP
    }

    HMSPaths.Entry ep2 = root.createAuthzObjPath(
        Lists.newArrayList("a", "b", "t", "p1", "p2"), "A");

    Assert.assertEquals(HMSPaths.EntryType.AUTHZ_OBJECT, entry.getType());
    Assert.assertEquals("p1", entry.getPathElement());
    Assert.assertEquals("A", entry.getAuthzObj());

    Assert.assertEquals(HMSPaths.EntryType.AUTHZ_OBJECT, ep2.getType());
    Assert.assertEquals("p2", ep2.getPathElement());
    Assert.assertEquals("A", entry.getAuthzObj());

    Assert.assertEquals(entry, root.find(new String[]{"a", "b", "t", "p1"},
        true));
    Assert.assertEquals(entry, root.find(new String[]{"a", "b", "t", "p1"},
        false));
    Assert.assertEquals(entry, root.find(new String[]{"a", "b", "t", "p1", "c"},
        true));
    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p1", "c"}, false));
    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "t", "p1")));

    Assert.assertEquals(ep2, root.find(new String[]{"a", "b", "t", "p1", "p2"},
        true));
    Assert.assertEquals(ep2, root.find(new String[]{"a", "b", "t", "p1", "p2"},
        false));
    Assert.assertEquals(ep2, root.find(new String[]{"a", "b", "t", "p1", "p2", "c"},
        true));
    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p1", "p2", "c"}, false));
    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "t", "p1", "p2")));

    root.find(new String[]{"a", "b", "t", "p1"}, false).delete();

    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p1"},
        true));
    Assert.assertEquals(HMSPaths.EntryType.DIR, entry.getType());
    Assert.assertNull(entry.getAuthzObj());

    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p1"}, false));
    Assert.assertNull(root.find(new String[]{"a", "b", "t"}, false));
    Assert.assertNull(root.find(new String[]{"a", "b"}, false));
    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "t", "p1")));

    Assert.assertNotNull(root.find(new String[]{"a", "b", "t", "p1", "p2"}, false));
    root.find(new String[]{"a", "b", "t", "p1", "p2"}, false).delete();
    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p1"}, false));
    Assert.assertNull(root.find(new String[]{"a", "b", "t"}, false));
    Assert.assertNull(root.find(new String[]{"a", "b"}, false));
    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "t", "p1")));

  }

  @Test
  public void testMultipleAuthzEntry() {
    HMSPaths.Entry root = HMSPaths.Entry.createRoot(false);
    HMSPaths.Entry prefix = root.createPrefix(Lists.newArrayList("a", "b"));

    HMSPaths.Entry e1 = root.createAuthzObjPath(
        Lists.newArrayList("a", "b", "t", "p1"), "A");
    HMSPaths.Entry e2 = root.createAuthzObjPath(
        Lists.newArrayList("a", "b", "t", "p2"), "A");


    Assert.assertEquals(e1, root.find(new String[]{"a", "b", "t", "p1"}, true));
    Assert.assertEquals(e1, root.find(new String[]{"a", "b", "t", "p1"}, 
        false));
    Assert.assertEquals(e1, root.find(new String[]{"a", "b", "t", "p1", "c"},
        true));
    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p1", "c"}, false));
    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "t", "p1")));

    Assert.assertEquals(e2, root.find(new String[]{"a", "b", "t", "p2"}, true));
    Assert.assertEquals(e2, root.find(new String[]{"a", "b", "t", "p2"}, 
        false));
    Assert.assertEquals(e2, root.find(new String[]{"a", "b", "t", "p2", "c"},
        true));
    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p2", "c"}, false));
    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "t", "p2")));

    root.find(new String[]{"a", "b", "t", "p1"}, true).delete();
    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p1"}, false));

    root.find(new String[]{"a", "b", "t", "p2"}, true).delete();
    Assert.assertNull(root.find(new String[]{"a", "b", "t", "p2"}, false));
    Assert.assertNull(root.find(new String[]{"a", "b", "t"}, false));

    Assert.assertEquals(prefix, root.findPrefixEntry(
        Lists.newArrayList("a", "b", "t", "p3")));
  }
  
}
