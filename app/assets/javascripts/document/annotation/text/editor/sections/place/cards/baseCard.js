define(['common/ui/formatting', 'common/utils/placeUtils',], function(Formatting, PlaceUtils) {

  var Card = function(element) {
    this.lastModifiedByEl = element.find('.last-modified .by');
    this.lastModifiedAtEl = element.find('.last-modified .at');
  };

  Card.TEMPLATES = {

    EDIT_BUTTONS :
      '<button class="change btn tiny">Change</button>' +
      '<button class="delete btn tiny icon">&#xf014;</button>',

    UNVERIFIED_WARNING_READ :
      '<span class="warning"><span class="icon">&#xf071;</span> Automatic Match</span>',

    UNVERIFIED_WARNING_WRITE :
      '<span class="warning"><span class="icon">&#xf071;</span> Automatic Match</span>' +
      '<button class="btn tiny delete icon">&#xf014;</button>' +
      '<button class="btn tiny unverified-change">Change</button>' +
      '<button class="btn tiny unverified-confirm">Confirm</button>'

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
