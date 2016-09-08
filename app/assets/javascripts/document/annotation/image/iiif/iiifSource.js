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
        quality = options.quality || 'native',
        width = options.width,
        height = options.height,
        tileSize = options.tileSize || 256;

    var ceil_log2 = function(x) {
      return Math.ceil(Math.log(x) / Math.LN2);
    };

    var maxZoom = Math.max(ceil_log2(width / tileSize),
                           ceil_log2(height / tileSize));

    var tierSizes = [];
    for (var i = 0; i <= maxZoom; i++) {
      var scale = Math.pow(2, maxZoom - i);
      var width_ = Math.ceil(width / scale);
      var height_ = Math.ceil(height / scale);
      var tilesX_ = Math.ceil(width_ / tileSize);
      var tilesY_ = Math.ceil(height_ / tileSize);
      tierSizes.push([tilesX_, tilesY_]);
    }

    var tilePixelRatio = Math.min((window.devicePixelRatio || 1), 4);

    var logicalTileSize = tileSize / tilePixelRatio;
    var logicalResolutions = tilePixelRatio == 1 ? options.resolutions :
        options.resolutions.map(function(el, i, arr) {
          return el * tilePixelRatio;
        });

    var modulo = function(a, b) {
      var r = a % b;
        return r * b < 0 ? r + b : r;
    };

    ol.source.TileImage.apply(this, [{
      tilePixelRatio: tilePixelRatio,
      tileGrid: new ol.tilegrid.TileGrid({
        resolutions: logicalResolutions.reverse(),
        origin: [0, 0],
        tileSize: logicalTileSize
      }),

      tileUrlFunction: function(tileCoord, pixelRatio, projection) {
        var z = tileCoord[0];
        if (maxZoom < z) return undefined;

        var sizes = tierSizes[z];
        if (!sizes) return undefined;

        var x = tileCoord[1];
        var y = -tileCoord[2] - 1;
        if (x < 0 || sizes[0] <= x || y < 0 || sizes[1] <= y) {
          return undefined;
        } else {
          var scale = Math.pow(2, maxZoom - z);
          var tileBaseSize = tileSize * scale;
          var minx = x * tileBaseSize;
          var miny = y * tileBaseSize;
          var maxx = Math.min(minx + tileBaseSize, width);
          var maxy = Math.min(miny + tileBaseSize, height);

          maxx = scale * Math.floor(maxx / scale);
          maxy = scale * Math.floor(maxy / scale);

          var query = '/' + minx + ',' + miny + ',' +
              (maxx - minx) + ',' + (maxy - miny) +
              '/pct:' + (100 / scale) + '/0/' + quality + '.' + extension;

          var url;
          if (jQuery.isArray(baseUrl)) {
            var hash = (x << z) + y;
            url = baseUrl[modulo(hash, baseUrl.length)];
          } else {
            url = baseUrl;
          }

          return url + query;
        }
      },
      crossOrigin: options.crossOrigin
    }]);

    /*
    if (ol.has.CANVAS) {
      this.setTileLoadFunction(function(tile, url) {
        var img = tile.getImage();
        goog.events.listenOnce(img, goog.events.EventType.LOAD, function() {
          if (img.naturalWidth > 0 &&
              (img.naturalWidth != tileSize || img.naturalHeight != tileSize)) {

            var canvas = goog.dom.createElement(goog.dom.TagName.CANVAS);
            canvas.width = tileSize;
            canvas.height = tileSize;

            var ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0);

            var key = goog.object.findKey(tile, function(v) {return v == img;});
            if (key) tile[key] = canvas;
          }

        }, true);
        img.src = url;
      });
    }*/

  };
  IIIFSource.prototype = Object.create(ol.source.TileImage.prototype);

  return IIIFSource;

});
