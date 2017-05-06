define([
  'common/api',
  'common/config',
  'common/ui/formatting',
  'common/utils/annotationUtils',
  'common/utils/placeUtils'], function(API, Config, Formatting, AnnotationUtils, PlaceUtils) {

  var SLIDE_DURATION = 250;

  var MapPopup = function(marker, place, annotations) {

    var element = jQuery(
          '<div class="popup">' +
            '<div class="popup-header"><h3></h3></div>' +
            '<div class="snippet">' +
              '<div class="snippet-body">' +
                '<div class="previous"><span class="icon stroke7">&#xe686;</span></div>' +
                '<div class="snippet-text">' +
                  '<div class="card"></div>' +
                '</div>' +
                '<div class="next"><span class="icon stroke7">&#xe684;</span></div>' +
              '</div>' +
              '<div class="snippet-footer">' +
                '<span class="label"></span>' +
                '<a class="jump-to-text"></a>' +
              '</div>' +
            '</div>' +
            '<div><table class="gazetteer-records"></table></div>' +
          '</div>'),

        titleEl        = element.find('.popup-header h3'),
        snippetTextEl  = element.find('.snippet-text'),
        snippetLabelEl = element.find('.label'),
        snippetLinkEl  = element.find('.jump-to-text'),

        btnPrev = element.find('.snippet-body .previous .icon'),
        btnNext = element.find('.snippet-body .next .icon'),

        currentAnnotationIdx = 0,

        fetchingSnippet = false,

        /** Helper **/
        getDistinct = function(arr) {
          return arr.reduce(function(distinct, elem) {
            if (distinct.indexOf(elem) < 0)
              distinct.push(elem);
            return distinct;
          }, []);
        },

        quotes = jQuery.map(annotations, function(annotation) {
          return AnnotationUtils.getQuote(annotation);
        }),

        distinctQuotes = getDistinct(quotes),

        transcriptions = jQuery.map(annotations, function(annotation) {
          return AnnotationUtils.getTranscriptions(annotation);
        }),

        distinctTranscriptions = getDistinct(transcriptions),

        distinctURIs = (function() {
          var distinctURIs = [];

          jQuery.each(annotations, function(i, a) {
            var placeBodies = AnnotationUtils.getBodiesOfType(a, 'PLACE');
            jQuery.each(placeBodies, function(j, b) {
              if (b.uri && distinctURIs.indexOf(b.uri) < 0)
                distinctURIs.push(b.uri);
            });
          });

          return distinctURIs;
        })(),

        getContentLink = function(annotation) {
          return jsRoutes.controllers.document.annotation.AnnotationController
            .resolveAnnotationView(Config.documentId, annotation.annotates.filepart_id, annotation.annotation_id).url;
        },

        showCard = function(annotation, slideDirection) {
          var createTextSnippet = function() {
                var quote = AnnotationUtils.getQuote(annotation),
                    offset = annotation.context.char_offset,
                    snippet = annotation.context.snippet;

                return snippet.substring(0, offset) + '<em>' +
                  snippet.substring(offset, offset + quote.length) + '</em>' +
                  snippet.substring(offset + quote.length);
              },

              createImageSnippet = function() {
                return'<div class="image-preview"><img src="' +
                  jsRoutes.controllers.api.AnnotationAPIController
                    .getImage(annotation.annotation_id).absoluteURL() + '"></div>';
              },

              createDataSnippet = function() {
                var table = jQuery('<table class="data-preview"></table>');
                jQuery.each(annotation.context, function(key, value) {
                  table.append('<tr><td>' + key + '</td><td>' + value + '</td></tr>');
                });
                return table;
              },

              currentCard = element.find('.snippet-text .card').first(),

              label = (currentAnnotationIdx + 1) + ' OF ' + annotations.length + ' ANNOTATIONS',

              linkText = (annotation.annotates.content_type.indexOf('IMAGE') > -1) ?
                'JUMP TO IMAGE' :
                'JUMP TO TEXT',

              snippet =
                (annotation.annotates.content_type.indexOf('TEXT') >= 0) ?
                  createTextSnippet() :
                ((annotation.annotates.content_type.indexOf('IMAGE') >= 0) ?
                  createImageSnippet() : createDataSnippet()),

              newCard, moveCurrentTo;

          snippetLabelEl.html(label);
          snippetLinkEl.html(linkText);
          snippetLinkEl.attr('href', getContentLink(annotation));

          if (!slideDirection) {
            // No slide - just replace
            currentCard.html(snippet);
            fetchingSnippet = false;
          } else {
            scrolling = true;
            newCard = jQuery('<div class="card"></div>');
            newCard.html(snippet);

            if (slideDirection === 'NEXT') {
              newCard.css('left', '100%');
              moveCurrentTo = '-100%';
            } else {
              newCard.css('left', '-100%');
              moveCurrentTo = '100%';
            }

            snippetTextEl.append(newCard);

            newCard.animate({ left: '0' }, SLIDE_DURATION, 'linear');

            currentCard.animate({ left: moveCurrentTo }, SLIDE_DURATION, 'linear', function() {
              currentCard.remove();
              fetchingSnippet = false;
            });
          }
        },

        /**
         * Fetches the preview snippet for an annotation via the API
         *
         * TODO needs to be revised once we have image snippets as well.
         */
        fetchSnippet = function() {
          return API.getAnnotation(annotations[currentAnnotationIdx].annotation_id, true);
        },

        onNextSnippet = function() {
          if (!fetchingSnippet) {
            fetchingSnippet = true;
            currentAnnotationIdx = (currentAnnotationIdx + 1) % annotations.length;
            fetchSnippet().done(function(annotation) { showCard(annotation, 'NEXT'); });
          }
        },

        onPreviousSnippet = function() {
          if (!fetchingSnippet) {
            fetchingSnippet = true;
            currentAnnotationIdx = (annotations.length + currentAnnotationIdx - 1) % annotations.length;
            fetchSnippet().done(function(annotation) { showCard(annotation, 'PREV'); });
          }
        },

        /**
         * Renders the title, depending on content type. For the time being,
         * it's safe to assume there are EITHER quotes OR transcriptions, but
         * never both.
         */
        renderTitle = function() {
          if (distinctQuotes.length > 0)
            titleEl.html(distinctQuotes.join(', '));
          else
            titleEl.html(distinctTranscriptions.join(', '));
        },

        render = function() {

          console.log(place, annotations, distinctURIs);

          renderTitle();

          jQuery.each(distinctURIs, function(idx, uri) {
            var record = PlaceUtils.getRecord(place, uri);

            console.log(record);

            var recordId = PlaceUtils.parseURI(record.uri),
                tr = jQuery(
                  '<tr data-uri="' + record.uri + '">' +
                    '<td class="record-id">' +
                      '<span class="shortcode"></span>' +
                      '<span class="id"></span>' +
                    '</td>' +
                    '<td class="place-details">' +
                      '<h3>' + record.title + '</h3>' +
                      '<p class="description"></p>' +
                      '<p class="date"></p>' +
                    '</td>' +
                  '</tr>');

            if (recordId.shortcode) {
              tr.find('.shortcode').html(recordId.shortcode);
              tr.find('.id').html(recordId.id);
              tr.find('.record-id').css('background-color', recordId.color);
            }

            if (record.descriptions && record.descriptions.length > 0)
              tr.find('.description').html(record.descriptions[0].description);
            else
              tr.find('.description').hide();

            if (record.temporal_bounds)
              tr.find('.date').html(
                Formatting.yyyyMMddToYear(record.temporal_bounds.from) + ' - ' +
                Formatting.yyyyMMddToYear(record.temporal_bounds.to));
            else
              tr.find('.date').hide();

            element.find('.gazetteer-records').append(tr);
          });

          fetchSnippet().done(function(annotation) { showCard(annotation); });

          if (annotations.length > 1) {
            btnPrev.click(onPreviousSnippet);
            btnNext.click(onNextSnippet);
          } else {
            btnNext.hide();
            btnPrev.hide();
          }
        },

        addTo = function(map) {
          render();
          marker.bindPopup(element[0]).openPopup();
        };

    this.addTo = addTo;
  };

  return MapPopup;

});
