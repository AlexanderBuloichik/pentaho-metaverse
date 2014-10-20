/*!
 * PENTAHO CORPORATION PROPRIETARY AND CONFIDENTIAL
 *
 * Copyright 2002 - 2014 Pentaho Corporation (Pentaho). All rights reserved.
 *
 * NOTICE: All information including source code contained herein is, and
 * remains the sole property of Pentaho and its licensors. The intellectual
 * and technical concepts contained herein are proprietary and confidential
 * to, and are trade secrets of Pentaho and may be covered by U.S. and foreign
 * patents, or patents in process, and are protected by trade secret and
 * copyright laws. The receipt or possession of this source code and/or related
 * information does not convey or imply any rights to reproduce, disclose or
 * distribute its contents, or to manufacture, use, or sell anything that it
 * may describe, in whole or in part. Any reproduction, modification, distribution,
 * or public display of this information without the express written authorization
 * from Pentaho is strictly prohibited and in violation of applicable laws and
 * international treaties. Access to the source code contained herein is strictly
 * prohibited to anyone except those individuals and entities who have executed
 * confidentiality and non-disclosure agreements or other agreements with Pentaho,
 * explicitly covering such access.
 */

package com.pentaho.metaverse.analyzer.kettle;

import com.pentaho.dictionary.DictionaryConst;
import com.pentaho.metaverse.testutils.MetaverseTestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.platform.api.metaverse.IMetaverseBuilder;
import org.pentaho.platform.api.metaverse.IMetaverseComponentDescriptor;
import org.pentaho.platform.api.metaverse.IMetaverseNode;
import org.pentaho.platform.api.metaverse.IMetaverseObjectFactory;
import org.pentaho.platform.api.metaverse.INamespace;
import org.pentaho.platform.api.metaverse.MetaverseAnalyzerException;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * This class tests the DatabaseConnectionAnalyzer methods.
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseConnectionAnalyzerTest {

  DatabaseConnectionAnalyzer dbConnectionAnalyzer;

  @Mock
  private DatabaseMeta databaseMeta;

  @Mock
  private IMetaverseBuilder builder;

  @Mock
  private INamespace namespace;

  @Mock
  private IMetaverseComponentDescriptor mockDescriptor;

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    IMetaverseObjectFactory factory = MetaverseTestUtils.getMetaverseObjectFactory();
    when( builder.getMetaverseObjectFactory() ).thenReturn( factory );

    dbConnectionAnalyzer = new DatabaseConnectionAnalyzer();
    dbConnectionAnalyzer.setMetaverseBuilder( builder );
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testSetMetaverseBuilder() {
    assertNotNull( dbConnectionAnalyzer.metaverseBuilder );
  }

  @Test
  public void testSetMetaverseObjectFactory() {
    assertNotNull( dbConnectionAnalyzer.metaverseObjectFactory );
  }

  @Test(expected = MetaverseAnalyzerException.class)
  public void testSetMetaverseBuilderNull() throws MetaverseAnalyzerException {

    dbConnectionAnalyzer.setMetaverseBuilder( null );
    dbConnectionAnalyzer.analyze( mockDescriptor, databaseMeta );
  }

  @Test(expected = MetaverseAnalyzerException.class)
  public void testSetMetaverseBuilderNullMetaverseObjectFactory() throws MetaverseAnalyzerException {
    dbConnectionAnalyzer.metaverseObjectFactory = null;
    dbConnectionAnalyzer.analyze( mockDescriptor, databaseMeta );
  }

  @Test
  public void testAnalyze() {
    when( builder.addNode( any( IMetaverseNode.class ) ) ).thenAnswer( new Answer<Object>() {
      @Override public Object answer( InvocationOnMock invocation ) throws Throwable {
        Object[] args = invocation.getArguments();
        // add the logicalId to the node like it does in the real builder
        IMetaverseNode node = (IMetaverseNode)args[0];
        node.setProperty( DictionaryConst.PROPERTY_LOGICAL_ID, node.getLogicalId() );
        return builder;
      }
    } );
    try {
      IMetaverseNode node = dbConnectionAnalyzer.analyze( mockDescriptor, databaseMeta );
      assertNotNull( node );
      assertEquals( 13, node.getPropertyKeys().size() );
    } catch ( MetaverseAnalyzerException e ) {
      fail( "analyze() should not throw an exception!" );
    }

  }

  @Test(expected = MetaverseAnalyzerException.class)
  public void testNullAnalyze() throws MetaverseAnalyzerException {

    dbConnectionAnalyzer.analyze( null, null );

  }

}
