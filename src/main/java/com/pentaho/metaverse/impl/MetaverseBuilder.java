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

package com.pentaho.metaverse.impl;

import com.pentaho.dictionary.DictionaryConst;
import com.pentaho.dictionary.DictionaryHelper;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.pentaho.platform.api.metaverse.IMetaverseBuilder;
import org.pentaho.platform.api.metaverse.IMetaverseLink;
import org.pentaho.platform.api.metaverse.IMetaverseNode;
import org.pentaho.platform.api.metaverse.IMetaverseObjectFactory;
import org.pentaho.platform.engine.core.system.PentahoSystem;

/**
 * @author mburgess
 */
public class MetaverseBuilder extends MetaverseObjectFactory implements IMetaverseBuilder {

  private static final String ENTITY_PREFIX = "entity_";

  private static final String ENTITY_NODE_ID = "entity";

  private static final String SEPARATOR = "~";

  private final Graph graph;

  /**
   * This is a possible delegate reference to a metaverse object factory. This builder is itself a
   * metaverse object factory, so the reference is initialized to "this".
   */
  private IMetaverseObjectFactory metaverseObjectFactory = this;

  /**
   * Instantiates a new Metaverse builder.
   *
   * @param graph the Graph to write to
   */
  public MetaverseBuilder( Graph graph ) {
    this.graph = graph;
  }

  /**
   * Retrieves the underlying graph object for this metaverse.
   *
   * @return
   */
  protected Graph getGraph() {
    return graph;
  }

  /**
   * Adds a link between 2 nodes in the underlying graph. If either node does not exist, it will be created.
   *
   * @param link the link to add
   * @return the builder
   */
  @Override
  public IMetaverseBuilder addLink( IMetaverseLink link ) {

    // make sure the from and to nodes exist in the graph
    Vertex fromVertex = getVertexForNode( link.getFromNode() );
    Vertex toVertex = getVertexForNode( link.getToNode() );
    String edgeId;

    // add the "from" vertex to the graph if it wasn't found
    if ( fromVertex == null ) {
      fromVertex = addVertex( link.getFromNode() );
      // set the virtual node property to true since this is an implicit adding of a node
      fromVertex.setProperty( DictionaryConst.NODE_VIRTUAL, true );
    }
    // update the vertex properties from the fromNode
    copyNodePropertiesToVertex( link.getFromNode(), fromVertex );

    // add the "to" vertex to the graph if it wasn't found
    if ( toVertex == null ) {
      toVertex = addVertex( link.getToNode() );
      // set the virtual node property to true since this is an implicit adding of a node
      toVertex.setProperty( DictionaryConst.NODE_VIRTUAL, true );
    }
    // update the to vertex properties from the toNode
    copyNodePropertiesToVertex( link.getToNode(), toVertex );

    addLink( fromVertex, link.getLabel(), toVertex );
    return this;
  }

  /**
   * Returns the edge id for the given vertices and edge label.
   *
   * @param fromVertex the source vertex
   * @param label      the edge label
   * @param toVertex   the target vertex
   * @return the String edge ID
   */
  protected String getEdgeId( Vertex fromVertex, String label, Vertex toVertex ) {
    String fromLogicalId = fromVertex.getProperty( DictionaryConst.PROPERTY_LOGICAL_ID );
    String toLogicalId = toVertex.getProperty( DictionaryConst.PROPERTY_LOGICAL_ID );

    StringBuilder sb = new StringBuilder();
    if( fromLogicalId != null ) {
      sb.append( fromLogicalId );
    } else {
      sb.append( fromVertex.getId() );
    }
    sb.append( SEPARATOR );
    if( toLogicalId != null ) {
      sb.append( toLogicalId );
    } else {
      sb.append( toVertex.getId() );
    }
    return sb.toString();
  }

  /**
   * Add a node to the underlying graph. If the node already exists, it's properties will get updated
   *
   * @param node the node to add
   * @return the builder
   */
  @Override
  public IMetaverseBuilder addNode( IMetaverseNode node ) {
    // does the node already exist?
    Vertex v = getVertexForNode( node );

    if ( v == null ) {
      // it's a new node, add it to the graph
      v = addVertex( node );
    }

    // adding this node means that it is no longer a virtual node
    v.setProperty( DictionaryConst.NODE_VIRTUAL, false );

    copyNodePropertiesToVertex( node, v );

    return this;
  }

  /**
   * adds a node as a Vertex in the graph
   *
   * @param node node to add as a Vertex
   * @return the Vertex added
   */
  private Vertex addVertex( IMetaverseNode node ) {

    Vertex v = graph.addVertex( node.getStringID() );

    if ( DictionaryHelper.isEntityType( node.getType() ) ) {
      Vertex entityType = addEntityType( node );

      // add a link from the entity type to the new node
      graph.addEdge( null, entityType, v, DictionaryConst.LINK_PARENT_CONCEPT );
    }

    return v;
  }

  /**
   * Adds an entity type node to the metaverse.
   *
   * @param node the metaverse node containing the entity information
   */
  protected Vertex addEntityType( IMetaverseNode node ) {

    // the node is an entity, so link it to its entity type node
    String nodeType = node.getType();
    Vertex entityType = graph.getVertex( ENTITY_PREFIX + nodeType );
    if ( entityType == null ) {
      // the entity type node does not exist, so create it
      entityType = graph.addVertex( ENTITY_PREFIX + nodeType );
      entityType.setProperty( DictionaryConst.PROPERTY_TYPE, DictionaryConst.NODE_TYPE_ENTITY );
      entityType.setProperty( DictionaryConst.PROPERTY_NAME, nodeType );

      // TODO move this to a map of types to strings or something
      if ( nodeType.equals( DictionaryConst.NODE_TYPE_TRANS ) || nodeType.equals( DictionaryConst.NODE_TYPE_JOB ) ) {
        entityType.setProperty( DictionaryConst.PROPERTY_DESCRIPTION, "Pentaho Data Integration" );
      }

      Vertex rootEntity = graph.getVertex( ENTITY_NODE_ID );
      if ( rootEntity == null ) {
        // the root entity node does not exist, so create it
        rootEntity = createRootEntity();
      }

      // add the link from the root node to the entity type
      addLink( rootEntity, DictionaryConst.LINK_PARENT_CONCEPT, entityType );

    }
    return entityType;
  }

  /**
   * Creates the root entity for this metaverse.
   */
  protected Vertex createRootEntity() {
    Vertex rootEntity = graph.addVertex( ENTITY_NODE_ID );
    rootEntity.setProperty( DictionaryConst.PROPERTY_TYPE, DictionaryConst.NODE_TYPE_ROOT_ENTITY );
    rootEntity.setProperty( DictionaryConst.PROPERTY_NAME, "METAVERSE" );

    // TODO get these properties from somewhere else
    rootEntity.setProperty( "division", "Engineering" );
    rootEntity.setProperty( "project", "Pentaho Data Lineage" );
    rootEntity.setProperty( "description",
        "Data lineage is tracing the path that data has traveled upstream from its destination, through Pentaho "
            + "systems and artifacts as well as external systems and artifacts." );

    return rootEntity;
  }

  /**
   * Copies all properties from a node into the properties of a Vertex
   *
   * @param node node with properties desired in a Vertex
   * @param v    Vertex to set properties on
   */
  private void copyNodePropertiesToVertex( IMetaverseNode node, Vertex v ) {

    // don't copy the node logicalId to a vertex if the node is virtual and the vertex is not
    Boolean nodeIsVirtual = (Boolean) node.getProperty( DictionaryConst.NODE_VIRTUAL );
    nodeIsVirtual = nodeIsVirtual == null ? true : nodeIsVirtual;

    Boolean vertexIsVirtual = v.getProperty( DictionaryConst.NODE_VIRTUAL );
    vertexIsVirtual = vertexIsVirtual == null ? false : vertexIsVirtual;

    String vertexLogicalId = v.getProperty( DictionaryConst.PROPERTY_LOGICAL_ID );
    boolean skipLogicalId = false;
    if ( vertexLogicalId != null && nodeIsVirtual && !vertexIsVirtual ) {
      skipLogicalId = true;
    }

    // set all of the properties, except the id and virtual (since that is an internally set prop)
    for ( String propertyKey : node.getPropertyKeys() ) {
      if ( !propertyKey.equals( DictionaryConst.PROPERTY_ID )
        && !propertyKey.equals( DictionaryConst.NODE_VIRTUAL )
        && !( skipLogicalId && propertyKey.equals( DictionaryConst.PROPERTY_LOGICAL_ID ) ) ) {
        Object value = node.getProperty( propertyKey );
        if ( value != null ) {
          v.setProperty( propertyKey, value );
        }
      }
    }
    node.setDirty( false );
  }

  /**
   * Helper method to get a Vertex from a node
   *
   * @param node the node to find a corresponding Vertex for
   * @return a matching Vertex or null if none found
   */
  protected Vertex getVertexForNode( IMetaverseNode node ) {
    if ( node != null && node.getStringID() != null ) {
      String logicalId = node.getLogicalId();
      Vertex vertex = graph.getVertex( node.getStringID() );

      if ( vertex == null && !logicalId.equals( node.getStringID() ) ) {
        // check for matching logicalIds
        Iterable<Vertex> logicalMatches = graph.getVertices( DictionaryConst.PROPERTY_LOGICAL_ID, logicalId );
        for ( Vertex match : logicalMatches ) {
          // just return the first match for now
          vertex = match;
          break;
        }
      }

      return vertex;
    } else {
      return null;
    }
  }

  @Override
  public IMetaverseBuilder deleteLink( IMetaverseLink link ) {
    deleteLink( link, true );
    return this;
  }

  /**
   * Deletes the specific link from the metaverse model and optionally removing virtual nodes associated
   * with the link
   *
   * @param link               the link to remove
   * @param removeVirtualNodes should any virtual nodes be removed or not?
   * @return true/false if the delete happened
   */
  private boolean deleteLink( IMetaverseLink link, boolean removeVirtualNodes ) {
    if ( link == null ) {
      return false;
    }

    // is there an edge in the graph that corresponds to the link?
    Vertex fromVertex = getVertexForNode( link.getFromNode() );
    Vertex toVertex = null;
    Edge deleteMe = null;
    boolean result = false;

    if ( fromVertex != null ) {

      // find all of the OUT linked Vertex's from this node
      for ( Edge edge : fromVertex.getEdges( Direction.OUT, link.getLabel() ) ) {
        // if the IN vertex's id matches the toNode's id, then we have a matching edge
        toVertex = edge.getVertex( Direction.IN );
        if ( toVertex.getId().equals( link.getToNode().getStringID() ) ) {
          // matching link found
          deleteMe = edge;
          break;
        }
      }

      if ( deleteMe != null ) {
        graph.removeEdge( deleteMe );
        result = true;
      }

      // now remove any "virtual" nodes associated with the link
      if ( removeVirtualNodes ) {
        Vertex[] fromAndTo = new Vertex[] { fromVertex, toVertex };
        for ( Vertex v : fromAndTo ) {
          if ( isVirtual( v ) ) {
            graph.removeVertex( v );
          }
        }
      }
    }
    return result;
  }

  @Override
  public IMetaverseBuilder deleteNode( IMetaverseNode node ) {
    Vertex v = getVertexForNode( node );
    if ( v != null ) {
      graph.removeVertex( v );
    }
    return this;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.pentaho.platform.api.metaverse.IMetaverseBuilder#updateLink(org.pentaho.platform.api.metaverse.IMetaverseLink)
   */
  @Override
  public IMetaverseBuilder updateLinkLabel( IMetaverseLink link, String label ) {
    if ( label != null && deleteLink( link, false ) ) {
      link.setLabel( label );
      addLink( link );
    }
    return this;
  }

  @Override
  public IMetaverseObjectFactory getMetaverseObjectFactory() {

    // Attempt to initialize the factory if it does not yet exist (or has been reset to null)
    if ( metaverseObjectFactory == null ) {
      // Attempt to find an injected class
      IMetaverseObjectFactory pentahoSystemMetaverseObjectFactory = PentahoSystem.get( IMetaverseObjectFactory.class );
      if ( pentahoSystemMetaverseObjectFactory != null ) {
        metaverseObjectFactory = pentahoSystemMetaverseObjectFactory;
      } else {
        // Default to ourselves (we are a subclass of MetaverseObjectFactory)
        metaverseObjectFactory = this;
      }
    }
    return metaverseObjectFactory;
  }

  @Override
  public void setMetaverseObjectFactory( IMetaverseObjectFactory metaverseObjectFactory ) {
    this.metaverseObjectFactory = metaverseObjectFactory;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.pentaho.platform.api.metaverse.IMetaverseBuilder#updateNode(org.pentaho.platform.api.metaverse.IMetaverseNode)
   */
  @Override
  public IMetaverseBuilder updateNode( IMetaverseNode node ) {

    Vertex v = getVertexForNode( node );
    if ( v != null ) {
      copyNodePropertiesToVertex( node, v );
    }

    return this;
  }

  /**
   * Adds the specified link to the model
   *
   * @param fromNode the from node
   * @param label    the label
   * @param toNode   the to node
   * @return this metaverse builder
   * @see org.pentaho.platform.api.metaverse.IMetaverseBuilder#addLink(
   *org.pentaho.platform.api.metaverse.IMetaverseNode,
   * java.lang.String,
   * org.pentaho.platform.api.metaverse.IMetaverseNode)
   */
  @Override
  public IMetaverseBuilder addLink( IMetaverseNode fromNode, String label, IMetaverseNode toNode ) {
    IMetaverseLink link = createLinkObject();

    link.setFromNode( fromNode );
    link.setLabel( label );
    link.setToNode( toNode );
    return addLink( link );
  }

  protected void addLink( Vertex fromVertex, String label, Vertex toVertex ) {
    String edgeId = getEdgeId( fromVertex, label, toVertex );
    // only add the link if the edge doesn't already exist
    if ( graph.getEdge( edgeId ) == null ) {
      Edge e = graph.addEdge( edgeId, fromVertex, toVertex, label );
      e.setProperty( "text", label );
    }
  }

  /**
   * determines if the node passed in is a virtual node
   * (meaning it has been implicitly added to the graph by an addLink)
   *
   * @param vertex node to determine if it is virtual
   * @return true/false
   */
  protected boolean isVirtual( Vertex vertex ) {
    if ( vertex == null ) {
      return false;
    }

    Boolean isVirtual = vertex.getProperty( DictionaryConst.NODE_VIRTUAL );
    return isVirtual == null ? false : isVirtual;
  }

}
