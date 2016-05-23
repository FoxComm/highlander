/* @flow */

import React, { Component } from 'react';
import type { HTMLElement } from 'types';

import styles from './locations.css';

const LOCATION_DATA = [
  {
    title: 'New York',
    address: [
      'SoHo',
      '151 Avenue of the Americas 6th Floor',
      'New York City, NY 10013',
    ],
    digs: [
      'Mon - Sat 8am - 9pm',
      'Sun 10am - 8pm',
      '646 . 111 . 123',
    ],
    image: 'Locations_New_York.jpg',
  },
  {
    title: 'Los Angeles',
    address: [
      'Hollywood',
      '8300 Sunset Boulevard',
      'Los Angeles, CA 90291',
    ],
    digs: [
      'Mon - Sat 8am - 9pm',
      'Sun 10am - 8pm',
      '310 . 280 . 2055',
    ],
    image: 'Locations_LA.jpg',
  },
  {
    title: 'DC',
    address: [
      'Georgetown',
      '3225 M Street NW',
      'Washington, DC 20007',
    ],
    digs: [
      'Mon - Sat 8am - 9pm',
      'Sun 10am - 8pm',
      '202 . 618 . 5605',
    ],
    image: 'Locations_DC.jpg',
  },
  {
    title: 'Chicago',
    address: [
      'Lincoln Park',
      '851 W Armitage Avenue',
      'Chicago, IL 60614',
    ],
    digs: [
      'Mon - Sat 8am - 9pm',
      'Sun 10am - 8pm',
      '773 . 341 . 1890',
    ],
    image: 'Locations_Chicago.jpg',
  },
];

class Locations extends Component {
  renderInfo(info: Array<string>): Array<HTMLElement> {
    return info.map((line, i) => {
      return <span key={i}>{line}</span>;
    });
  }

  renderLocationList(): Array<HTMLElement> {
    return LOCATION_DATA.map((location, i) => {
      return (
        <li styleName="list__item" key={i}>
        <span styleName="image">
          <img src={`./images/locations/${location.image}`} alt={location.title} />
        </span>
        <span styleName="address">
          <h2 styleName="subtitle">{location.title}</h2>
          <p>{this.renderInfo(location.address)}</p>
        </span>
        <span styleName="digs">
          <h3 styleName="subtitle">The Digs</h3>
          <p>{this.renderInfo(location.digs)}</p>
        </span>
        <span styleName="actions">
          <button styleName="button">Get Directions</button>
        </span>
        </li>
      );
    });
  }

  render(): HTMLElement {
    return (
      <section styleName="locations">
        <header styleName="header">
          <h1 styleName="title">Retail Locations</h1>
        </header>
        <ul styleName="list">
          {this.renderLocationList()}
        </ul>
      </section>
    );
  }
}

export default Locations;
