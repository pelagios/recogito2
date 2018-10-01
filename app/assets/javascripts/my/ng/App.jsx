import React, { Component } from 'react';
import { render } from 'react-dom';
import axios from 'axios';

import Sidebar from './sidebar/Sidebar.jsx';
import Header from './header/Header.jsx';

import TablePane from './documents/table/TablePane.jsx';
import GridPane from './documents/grid/GridPane.jsx';
import UploadProgress from './documents/upload/UploadProgress.jsx';
import Readme from './documents/Readme.jsx';

export default class App extends Component {

  constructor(props) {
    super(props);
    this.state = {
      view: 'MY_DOCUMENTS',
      presentation: 'TABLE',
      documents: []
    };
  }

  componentDidMount() {
    axios.get('/my/documents.json')
      .then(result => {
        this.setState({ documents: result.data });
      });
  }

  changeView(view) {
    this.setState({view: view});
  }

  togglePresentation() {
    this.setState(before => {
      if(before.presentation == 'TABLE')
        return({ presentation: 'GRID' });
      else
        return({ presentation: 'TABLE' });
    });
  }

  onDropFile(files) {
    console.log('dropped files', files);
  }

  render() {
    return(
      <React.Fragment>
        <Sidebar
          currentView={this.state.view}
          onChangeView={this.changeView.bind(this)} />

        <div className="container">
          <Header
            presentation={this.state.presentation}
            onTogglePresentation={this.togglePresentation.bind(this)} />

          {this.state.presentation == 'TABLE' ?
            <TablePane
              folders={[]}
              documents={this.state.documents}>
              {/* <Readme>Hello World!</Readme> */}
            </TablePane>
            :
            <GridPane
              folders={[]}
              documents={this.state.documents}
              onDropFile={this.onDropFile.bind(this)}>
              {/* }<Readme>Hello World!</Readme> */}
            </GridPane>
          }
        </div>

        {/* <UploadProgress
          phase="Uploading"
          files={["odyssey-pt1.txt", "odyssey-pt2.txt", "odyssey-pt3.txt"]}
          progress={0.3} /> */}

      </React.Fragment>
    )
  }

}

render(<App />, document.getElementById('app'));
