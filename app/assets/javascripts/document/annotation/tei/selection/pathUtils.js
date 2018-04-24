define([], function() {

  return {

    // https://www.benpickles.com/articles/51-finding-a-dom-nodes-common-ancestor-using-javascript
    getCommonAncestor : function(node1, node2) {
      var parents = function(node) {
            var nodes = [ node ];
            for (; node; node = node.parentNode) {
              nodes.unshift(node);
            }
            return nodes;
          },

          parents1 = parents(node1),
          parents2 = parents(node2);

      // No common ancestor
      if (parents1[0] !== parents2[0]) return;

      for (var i=0; i<parents1.length; i++) {
        if (parents1[i] !== parents2[i]) return parents1[i - 1];
      }
    },

    getXPath : function(node, path) {
      var sibling, count;

      path = path || [];
      if(node.parentNode) {
        path = this.getXPath(node.parentNode, path);
      }

      if(node.previousSibling) {
        count = 1;
        sibling = node.previousSibling;
        do {
          if(sibling.nodeType == 1  && sibling.nodeName == node.nodeName) {count++;}
          sibling = sibling.previousSibling;
        } while(sibling);
        if(count == 1) {count = null;}
      } else if(node.nextSibling) {
        sibling = node.nextSibling;
        do {
          if(sibling.nodeType == 1 && sibling.nodeName == node.nodeName) {
               count = 1;
               sibling = null;
          } else {
            count = null;
            sibling = sibling.previousSibling;
          }
        } while(sibling);
      }

      if(node.nodeType == 1) {
        path.push(node.nodeName.toLowerCase() + (node.id ? "[@id='"+node.id+"']" : count > 0 ? "["+count+"]" : ''));
      }

      return path;
    },

    /**
     * Helper that transforms the CETEIcean-specific DOM XPath to a
     * normalized XPath with the TEI document only.
     */
    toTEIPaths : function(rootNode, startPath, endPath, selectedRange) {
      var pathStart = rootNode.nodeName.toLowerCase() + "[@id='" + rootNode.id + "']",

          // For a given node, returns the closest parent that is a TEI element
          getClosestTEINode = function(node) {
            if (node.nodeName.toLowerCase().indexOf('tei-') === 0)
              return node;
            else
              return getClosestTEINode(node.parentNode);
          },

          // Helper to compute char offsets between end of XPath and given selection bound
          getOffsetFromTo = function(fromNode, toNode, toOffset) {
            var range = rangy.createRange();
            range.setStart(fromNode, 0);
            range.setEnd(toNode, toOffset);
            return range.toString().length;
          },

          startOffset = getOffsetFromTo(
            getClosestTEINode(selectedRange.startContainer),
            selectedRange.startContainer,
            selectedRange.startOffset),

          endOffset = getOffsetFromTo(
            getClosestTEINode(selectedRange.endContainer),
            selectedRange.endContainer,
            selectedRange.endOffset),

          // Removes all refs to non-TEI nodes (i.e. those added by the Recogito view)
          fixPath = function(path) {
            return path.slice(path.indexOf(pathStart) + 1).reduce(function(xpath, p) {
                     if (p.indexOf('tei-') === 0)
                       return xpath + '/' + p.substring(4);
                     else
                       return xpath;
                   }, '');
          },

          startTEIPath = fixPath(startPath) + '::' + startOffset,
          endTEIPath = fixPath(endPath) + '::' + endOffset;

      return 'from=' + startTEIPath + ';to=' + endTEIPath;
    }

  };

});
