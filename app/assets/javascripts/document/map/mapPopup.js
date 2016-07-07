define([
  'common/api',
  'common/ui/formatting',
  'common/utils/annotationUtils',
  'common/utils/placeUtils'], function(API, Formatting, AnnotationUtils, PlaceUtils) {

  var SLIDE_DURATION = 250;

  var MapPopup = function(latlng, place, annotations) {

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
                '<!-- a class="jump-to-text" href="#" onclick="return false;">JUMP TO TEXT</a -->' +
              '</div>' +
            '</div>' +
            '<div><table class="gazetteer-records"></table></div>' +
          '</div>'),

        titleEl        = element.find('.popup-header h3'),
        snippetTextEl  = element.find('.snippet-text'),
        snippetLabelEl = element.find('.label'),

        btnPrev = element.find('.snippet-body .previous .icon'),
        btnNext = element.find('.snippet-body .next .icon'),

        currentSnippetIdx = 0,

        scrolling = false,

        quotes = jQuery.map(annotations, function(annotation) {
          return AnnotationUtils.getQuote(annotation);
        }),

        distinctQuotes = quotes.reduce(function(distinctQuotes, quote) {
          if (distinctQuotes.indexOf(quote) < 0)
            distinctQuotes.push(quote);
          return distinctQuotes;
        }, []),

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

        showCard = function(snippet, slideDirection) {
          var currentCard = element.find('.snippet-text .card').first(),
              label = (currentSnippetIdx + 1) + ' OF ' + annotations.length + ' ANNOTATIONS',
              newCard, moveCurrentTo;

          snippetLabelEl.html(label);

          if (!slideDirection) {
            // No slide - just replace
            currentCard.html(snippet);
          } else {
            scrolling = true;
            newCard = jQuery('<div class="card">' + snippet + '</snippet>');

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
              scrolling = false;
            });
          }
        },

        /**
         * Fetches the preview snippet for an annotation via the API
         *
         * TODO needs to be revised once we have image snippets as well.
         */
        fetchSnippet = function() {
          return API.getAnnotation(annotations[currentSnippetIdx].annotation_id, true)
             .then(function(annotation) {
               var offset = annotation.context.char_offset,
                   quote = AnnotationUtils.getQuote(annotation),
                   snippet = annotation.context.snippet,
                   formatted =
                     snippet.substring(0, offset) + '<em>' +
                     snippet.substring(offset, offset + quote.length) + '</em>' +
                     snippet.substring(offset + quote.length);

               return formatted;
           });
        },

        onNextSnippet = function() {
          if (!scrolling) {
            if (currentSnippetIdx < annotations.length - 1) {
              currentSnippetIdx += 1;
              fetchSnippet().done(function(snippet) { showCard(snippet, 'NEXT'); });
            }
          }
        },

        onPreviousSnippet = function() {
          if (!scrolling) {
            if (currentSnippetIdx > 0) {
              currentSnippetIdx -= 1;
              fetchSnippet().done(function(snippet) { showCard(snippet, 'PREV'); });
            }
          }
        },

        render = function() {
          titleEl.html(distinctQuotes.join(', '));

          jQuery.each(distinctURIs, function(idx, uri) {
            var record = PlaceUtils.getRecord(place, uri),
                recordId = PlaceUtils.parseURI(record.uri),
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

            if (record.descriptions.length > 0)
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

          fetchSnippet().done(showCard);

          if (annotations.length > 1) {
            btnPrev.click(onPreviousSnippet);
            btnNext.click(onNextSnippet);
          } else {
            btnPrev.hide();
            btnNext.hide();
          }
        },

        addTo = function(map) {
          render();
          L.popup().setLatLng(latlng).setContent(element[0]).openOn(map);
        };

    this.addTo = addTo;
  };

  return MapPopup;

});
