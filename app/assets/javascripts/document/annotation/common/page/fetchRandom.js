/** 
 * An experimental function that picks a random accessible 
 * document from the workspace of the current document's owner.
 * Optionally requires that 'my_annotations' on the document is
 * zero.
 */
define(['common/config'], function(Config) {
  
  return {
    
    fetch: function(requireZero) {
      return jQuery.ajax({
        url: '/api/directory/' + Config.documentOwner,
        type: 'POST',
        data: JSON.stringify({ columns: [ 'my_annotations' ]}),
        contentType: 'application/json'
      }).then(function(response) {
        // Filter for documents != current and (optionally) where my_annotations == 0
        var candidates = jQuery.grep(response.items, function(item) {
              var isDocument = item.type === 'DOCUMENT' && item.id != Config.documentId;
              return requireZero ? isDocument & item.my_annotations === 0 : isDocument;
            });

          console.log(response.items);
          console.log(candidates);
          
        return candidates.length > 0 ? candidates[Math.floor(Math.random() * candidates.length)] : null;
      });
    },

    createButton: function(item) {
      var url = item ? '/document/' + item.id + '/part/1/edit' : null,

          el = url ? jQuery(
            '<div class="suggest-random btn small">' +
              '<a href="' + url + '">Annotate Next</a>' +
            '<div>') : jQuery(
            '<div class="suggest-random btn small outline>' +
              '<span>Congratulations! No more documents to annotate.</span>' +
            '</div>');

      return el;
    }

  }

});