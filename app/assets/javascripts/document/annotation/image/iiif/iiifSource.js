/**
 * IIIF tile source implementation for OpenLayers, modified from the KlokanTech IIIFViewer at
 * https://raw.githubusercontent.com/klokantech/iiifviewer/master/src/iiifsource.js
 *
 * Please note below license text for KlokanTech's IIIFViewer.
 */
define([], function() {

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
  var IIIFSource = function(options) {

    var baseUrl = options.baseUrl,

        extension = options.extension || 'jpg',

        quality = options.quality || 'default',

        width = options.width,

        height = options.height,

        tileSize = options.tileSize || 256,

        maxZoom =
          Math.round(
            Math.log2(Math.max(width, height) / tileSize)
          ),

        tilePixelRatio = Math.min((window.devicePixelRatio || 1), 4),

        logicalTileSize = tileSize / tilePixelRatio,

        logicalResolutions = (tilePixelRatio == 1) ? options.resolutions :
          options.resolutions.map(function(el, i, arr) {
            return el * tilePixelRatio;
          }),

        tierSizes = (function() {
          var tierSizes = [], i;

          for (i = 0; i <= maxZoom; i++) {
            var scale = Math.pow(2, maxZoom - i),
                width_ = Math.ceil(width / scale),
                height_ = Math.ceil(height / scale),
                tilesX_ = Math.ceil(width_ / tileSize),
                tilesY_ = Math.ceil(height_ / tileSize);

            tierSizes.push([ tilesX_, tilesY_ ]);
          }

          return tierSizes;
        })();

    ol.source.TileImage.apply(this, [{

      tilePixelRatio: tilePixelRatio,

      tileGrid: new ol.tilegrid.TileGrid({
        resolutions: logicalResolutions.reverse(),
        origin: [ 0, 0 ],
        tileSize: logicalTileSize
      }),

      crossOrigin: options.crossOrigin,

      tileUrlFunction: function(tileCoord, pixelRatio) {
        var z = tileCoord[0],
            x = tileCoord[1],
            y = - tileCoord[2] - 1,
            sizes = tierSizes[z],

            modulo = function(a, b) {
              var r = a % b;
              return r * b < 0 ? r + b : r;
            };

        if (z > maxZoom)
          return undefined;

        if (!sizes)
          return undefined;

        if (x < 0 || y < 0 || sizes[0] < x || sizes[1] < y)
          return undefined;

        var scale = Math.pow(2, maxZoom - z),
            tileBaseSize = tileSize * scale,
            minx = x * tileBaseSize,
            miny = y * tileBaseSize,
            tileW = Math.min(tileBaseSize, width - minx),
            tileH = Math.min(tileBaseSize, height - miny),
            query, url, hash;

        query = '/' + minx + ',' + miny + ',' + tileW + ',' + tileH +
          '/pct:' + (100 / scale) + '/0/' + quality + '.' + extension;

        if (jQuery.isArray(baseUrl)) {
          hash = (x << z) + y;
          url = baseUrl[modulo(hash, baseUrl.length)];
        } else {
          url = baseUrl;
        }

        return url + query;
      }
    }]);

    if (ol.has.CANVAS) {
      this.setTileLoadFunction(function(tile, url) {
        var img = tile.getImage();

        img.addEventListener('load', function()  {
          if (img.naturalWidth > 0 &&
              (img.naturalWidth != tileSize || img.naturalHeight != tileSize)) {

            var canvas = document.createElement('CANVAS');
            canvas.width = tileSize;
            canvas.height = tileSize;

            var ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0);

            for (var key in tile) {
              var value = tile[key];
              if (value == img)
                tile[key] = canvas;
            }
          }

        }, true);

        img.src = url;
      });
    }
  };
  IIIFSource.prototype = Object.create(ol.source.TileImage.prototype);

  return IIIFSource;

});
