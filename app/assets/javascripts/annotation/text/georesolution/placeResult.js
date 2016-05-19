define([], function() {

  var placeResult = function(ul, placeItem) {

    var li = jQuery(
          '<li class="place-details">' +
            '<h3>' + placeItem.labels.join(', ') + '</h3>' +
            '<p class="description">An ancient place</p>' +
          '</li>');

    console.log(placeItem);
    ul.append(li);
  };

  return placeResult;

});
