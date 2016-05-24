define([
  '../../../common/helpers/formatting',
  '../../../common/helpers/placeUtils',
  '../../../common/hasEvents'], function(Formatting, PlaceUtils, HasEvents) {

  var PlaceResult = function(ul, place) {
    var self = this,

        li = (function() {
               var el = jQuery(
                 '<li class="place-details">' +
                   '<h3>' + place.labels.join(', ') + '</h3>' +
                   '<p class="gazetteer"></p>' +
                   '<p class="description"></p>' +
                   '<p class="date"></p>' +
                 '</li>'),

                 gazetteersEl = el.find('.gazetteer'),

                 descriptionEl = el.find('.description'),

                 dateEl = el.find('.date'),

                 uris = PlaceUtils.getURIs(place),

                 descriptions = PlaceUtils.getDescriptions(place);

               jQuery.each(uris, function(idx, uri) {
                 gazetteersEl.append(Formatting.formatGazetteerURI(uri));
               });

               if (descriptions.length > 0)
                 descriptionEl.html(descriptions[0].description);
               else
                 descriptionsEl.hide();

               if (place.temporal_bounds)
                 dateEl.html(Formatting.yyyyMMddToYear(place.temporal_bounds.from) + ' - ' +
                           Formatting.yyyyMMddToYear(place.temporal_bounds.to));
               else
                 dateEl.hide();

               return el;
             })();

    li.click(function() {
      self.fireEvent('click');
    });

    ul.append(li);

    HasEvents.apply(this);
  };
  PlaceResult.prototype = Object.create(HasEvents.prototype);

  return PlaceResult;

});
