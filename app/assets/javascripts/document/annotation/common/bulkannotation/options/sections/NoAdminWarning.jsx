import React, { Component } from 'react';
import { CSSTransition } from 'react-transition-group';

export default class NoAdminWarning extends Component {

  render() {
    return (
      <React.Fragment>
        {!window.config.isAdmin &&
          <CSSTransition
            classNames="no-admin-wrapper"
            in={this.props.visible}
            timeout={150}>

            <div className={(this.props.visible) ? "no-admin-wrapper" : "no-admin-wrapper no-admin-wrapper-exit-done"}>
              <div className="no-admin">
                <span className="icon">{'\uf071'}</span> You do not have
                administrator privileges for this document. This means you cannot
                replace annotations that contain comments by other users. Recogito
                will skip these annotations.
              </div>
            </div>
          </CSSTransition>
        }
      </React.Fragment>
    )
  }

}
