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

package com.pentaho.metaverse.locator;

import com.pentaho.metaverse.messages.Messages;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A runnable (and stoppable) class for crawling a Pentaho repository for documents
 * @author jdixon
 *
 */
public class RepositoryLocatorRunner extends LocatorRunner<List<RepositoryFileTree>> {

  private static final Logger LOG = LoggerFactory.getLogger( LocatorRunner.class );

  /**
   * Indexes a set of files/folders. Folders are recursed into and files are passed to indexFile.
   * @param fileTrees The files/folders to examine
   */
  @Override
  public void locate( List<RepositoryFileTree> fileTrees ) {

    for ( RepositoryFileTree fileTree : fileTrees ) {
      if ( stopping ) {
        return;
      }
      if ( fileTree.getFile() != null ) {
        RepositoryFile file = fileTree.getFile();
        if ( !file.isFolder() ) {

          if ( !file.isHidden() ) {
            // don't index hidden fields
            try {
              processFile( locator.getNamespace(), file.getName(), file.getPath(),  file );
            } catch ( Exception e ) {
              // something truly unexpected would have to have happened ... NPE or similar ugliness
              LOG.error( Messages.getString( "ERROR.ProcessFileFailed", file.getName() ), e );
            }
          }
        } else {
          List<RepositoryFileTree> kids = fileTree.getChildren();
          if ( kids != null && kids.size() > 0 ) {
            locate( kids );
          }
        }
      }

    }
  }

}
