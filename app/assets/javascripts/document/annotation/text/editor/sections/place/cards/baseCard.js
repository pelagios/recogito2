define(['common/ui/formatting'], function(Formatting) {

  var Card = function(element) {
    this.lastModifiedByEl = element.find('.last-modified .by');
    this.lastModifiedAtEl = element.find('.last-modified .at');
  };

  Card.prototype.formatURI = function(uri) {
    var parsed = PlaceUtils.parseURI(uri);
    if (parsed.shortcode)
      return '<a class="gazetteer-id" href="' + uri + '" target="_blank">' +
        parsed.shortcode + ':' + parsed.id + '</a>';
      else
        return '<a class="gazetteer-id" href="' + uri + '" target="_blank">' +
          uri + '</a>';
  };

  Card.prototype.setLastModified = function(lastModified) {
    this.lastModifiedByEl.html(lastModified.by);
    this.lastModifiedByEl.attr('href', '/' + lastModified.by);
    this.lastModifiedAtEl.html(Formatting.timeSince(lastModified.at));
  };

  return Card;

});
