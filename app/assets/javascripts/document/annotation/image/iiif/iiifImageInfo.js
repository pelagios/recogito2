/**
 * Helper to parse IIIF image info, based on KlokanTech IIIFViewer at
 * https://raw.githubusercontent.com/klokantech/iiifviewer/master/src/iiifviewer.js
 *
 * Please note below license text for KlokanTech's IIIFViewer.
 */
define(['common/ui/alert'], function(Alert) {

  /**
   * Copyright 2014-2015 Klokan Technologies GmbH (http://www.klokantech.com). All rights reserved.
   *
   * Redistribution and use in source and binary forms, with or without modification, are permitted
   * provided that the following conditions are met:
   *
   * Redistributions of source code must retain the above copyright notice, this list of
   * conditions and the following disclaimer.
   *
   * Redistributions in binary form must reproduce the above copyright notice, this list of
   * conditions and the following disclaimer in the documentation and/or other materials
   * provided with the distribution.
   *
   * THIS SOFTWARE IS PROVIDED BY KLOKAN TECHNOLOGIES GMBH AND OTHER CONTRIBUTORS ``AS IS''
   * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
   * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
   * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
   * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
   * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
   * OF THE POSSIBILITY OF SUCH DAMAGE.
   *
   * The views and conclusions contained in the software and documentation are those of the
   * authors and should not be interpreted as representing official policies, either expressed
   * or implied, of Klokan Technologies GmbH.
   */
  var IIIFImageInfo = function(json) {
    var getURL = function() {
          var id = json['@id'],
              host = json.image_host,
              identifier = json.identifier;

          if (id)
            return id;
          else if (host && identifier)
            return host + identifier;
        },

        computeResolutions = function(width, height, tilesize) {
          var resolutions = [], r_ = 1;
          while (Math.max(width, height) / r_ > tilesize) {
            resolutions.push(r_);
            r_ *= 2;
          }

          return resolutions;
        };

    this.width = json.width;
    this.height = json.height;
    this.baseUrl = getURL();
    this.tiles = (json.tiles || [{}])[0];
    this.resolutions = json.scale_factors || this.tiles.scaleFactors || [];
    this.extension = (json.formats || [])[0];
    this.tileSize = json.tile_width || this.tiles.width || 256;

    if (this.resolutions.length === 0)
      this.resolutions = computeResolutions(this.width, this.height, this.tileSize);

    if (!this.width || !this.height || !this.baseUrl)
      new Alert(Alert.ERROR, 'Invalid Manifest', 'Could not open the IIIF image. The manifest appears to be invalid.');
  };

  return IIIFImageInfo;

});
