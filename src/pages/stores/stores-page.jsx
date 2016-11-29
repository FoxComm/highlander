/* @flow */

// libs
import React from 'react';

// styles
import styles from './stores-page.css';
import mapStyles from './map-styles';

// components
import Button from 'ui/buttons';

export default class StoresPage extends React.Component {
  componentDidMount() {
    const { google } = window;
    const mapOptions = {
      zoom: 15,
      center: new google.maps.LatLng(39.4106519, -76.6452035),
      styles: mapStyles,
      zoomControl: true,
    };

    const map = new google.maps.Map(this.refs.map, mapOptions);
    const marker = new google.maps.Marker({
      position: new google.maps.LatLng(39.4106519, -76.6452035),
    });

    marker.setMap(map);
  }

  render() {
    return (
      <div>
        <header>
          <div styleName="header-wrap">
            <div styleName="text-wrap">
              <span styleName="description">
                Live in the Greater Baltimore area?
              </span>
              <h1 styleName="title">COME VISIT US!</h1>
            </div>
          </div>
        </header>
        <div styleName="main-block">
          <div styleName="main-block-wrapper">
            <h2 styleName="subtitle">
              Find us at Riderwood Village
            </h2>
            <div styleName="central-container">
              <div styleName="address-info-block">
                <div styleName="address-info-wrapper">
                  <div styleName="address-line">
                    <div>8012 Bellona Ave</div>
                    <div>Towson, MD 21204</div>
                    <div>(410) 325-4411</div>
                  </div>
                  <div styleName="working-hours">
                    <div>Mon - Fri: 10am - 5pm </div>
                    <div>Sat - Sun: 12pm - 4pm</div>
                  </div>
                </div>
                <a href="https://www.google.com/maps?saddr=My+Location&daddr=8012+Bellona+Ave+Towson+MD+21204" target="_blank">
                  <Button styleName="get-directions-btn">GET DIRECTIONS</Button>
                </a>
              </div>

              <div styleName="map-block" ref="map" />
            </div>
          </div>
        </div>
      </div>
    );
  }
}
