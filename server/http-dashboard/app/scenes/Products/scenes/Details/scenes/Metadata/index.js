import React from 'react';
import {BackTop} from 'components';
import MetadataComponents from 'scenes/Products/components/Metadata';
import {Metadata as MetadataFields, hardcodedRequiredMetadataFieldsNames} from 'services/Products';
const {ItemsList, Fields: {ContactField, TextField, NumberField, UnitField, TimeField, ShiftField, CostField, CoordinatesField, AddressField, LocationField, DeviceOwnerField, DeviceNameField}} = MetadataComponents;
class Metadata extends React.Component {

  static propTypes = {
    product: React.PropTypes.shape({
      metaFields: React.PropTypes.array
    }),
  };

  filterStaticFields(field) {
    const hardcodedNames = [
      hardcodedRequiredMetadataFieldsNames.DeviceName,
      hardcodedRequiredMetadataFieldsNames.DeviceOwner,
      hardcodedRequiredMetadataFieldsNames.LocationName,
    ];
    return hardcodedNames.indexOf(field.name) !== -1;
  }

  filterDynamicFields(field) {
    const hardcodedNames = [
      hardcodedRequiredMetadataFieldsNames.DeviceName,
      hardcodedRequiredMetadataFieldsNames.DeviceOwner,
      hardcodedRequiredMetadataFieldsNames.LocationName,
    ];
    return hardcodedNames.indexOf(field.name) === -1;
  }

  getFields() {

    const fields = [];

    if (!this.props.product.metaFields)
      return fields;

    this.props.product.metaFields.filter(this.filterDynamicFields).forEach((field, key) => {

      const props = {
        key: key
      };

      if (field.type === MetadataFields.Fields.TEXT) {
        fields.push(
          <TextField.Static
            {...props}
            name={field.name}
            value={field.value}
            role={field.role}
          />
        );
      }

      if (field.type === MetadataFields.Fields.NUMBER) {
        fields.push(
          <NumberField.Static
            {...props}
            name={field.name}
            value={field.value}
            role={field.role}
          />
        );
      }

      if (field.type === MetadataFields.Fields.UNIT) {
        fields.push(
          <UnitField.Static
            {...props}
            name={field.name}
            value={field.value}
            units={field.units}
            role={field.role}
          />
        );
      }

      if (field.type === MetadataFields.Fields.ADDRESS) {
        fields.push(
          <AddressField.Static
            {...props}
            name={field.name}
            role={field.role}
            streetAddress={field.streetAddress}
            city={field.city}
            state={field.state}
            zip={field.zip}
            country={field.country}
          />
        );
      }

      if (field.type === MetadataFields.Fields.TIME) {
        fields.push(
          <TimeField.Static
            {...props}
            name={field.name}
            time={field.time}
            role={field.role}
          />
        );
      }

      if (field.type === MetadataFields.Fields.RANGE) {
        fields.push(
          <ShiftField.Static
            {...props}
            name={field.name}
            from={field.from}
            to={field.to}
            role={field.role}
          />
        );
      }

      if (field.type === MetadataFields.Fields.COST) {
        fields.push(
          <CostField.Static
            {...props}
            name={field.name}
            price={field.price}
            perValue={field.perValue}
            currency={field.currency}
            units={field.units}
            role={field.role}
          />
        );
      }

      if (field.type === MetadataFields.Fields.COORDINATES) {
        fields.push(
          <CoordinatesField.Static
            {...props}
            name={field.name}
            lat={field.lat}
            lon={field.lon}
            role={field.role}
          />
        );
      }

      if (field.type === MetadataFields.Fields.CONTACT) {
        fields.push(
          <ContactField.Static
            {...props}
            role={field.role}
            fields={{
              name: field.name,
              isDefaultsEnabled: field.isDefaultsEnabled,
              fieldAvailable: field.fieldAvailable,
              values: {
                firstName: {
                  checked: field.isFirstNameEnabled,
                  value: field.firstName,
                },
                lastName: {
                  checked: field.isLastNameEnabled,
                  value: field.lastName,
                },
                email: {
                  checked: field.isEmailEnabled,
                  value: field.email,
                },
                phone: {
                  checked: field.isPhoneEnabled,
                  value: field.phone,
                },
                streetAddress: {
                  checked: field.isStreetAddressEnabled,
                  value: field.streetAddress,
                },
                city: {
                  checked: field.isCityEnabled,
                  value: field.city,
                },
                state: {
                  checked: field.isStateEnabled,
                  value: field.state,
                },
                zip: {
                  checked: field.isZipEnabled,
                  value: field.zip,
                }
              }
            }}
          />
        );
      }

    });

    return fields;

  }

  getStaticFields() {

    const elements = [];

    this.props.product.metaFields.filter(this.filterStaticFields).forEach((field, key) => {

      if (!field.name) return false;

      const props = {
        key: key,
        name: field.name,
        value: field.value,
        role: field.role
      };

      if (field.name && field.name === hardcodedRequiredMetadataFieldsNames.LocationName) {
        elements.push(
          <LocationField.Static {...props}/>
        );
      }

      if (field.name && field.name === hardcodedRequiredMetadataFieldsNames.DeviceOwner) {
        elements.push(
          <DeviceOwnerField.Static {...props}/>
        );
      }

      if (field.name && field.name === hardcodedRequiredMetadataFieldsNames.DeviceName) {
        elements.push(
          <DeviceNameField.Static {...props}/>
        );
      }

    });

    return elements;

  }

  render() {

    if (!this.getFields().length && !this.getStaticFields().length) return (
      <div className="product-no-fields">No metadata fields</div>
    );

    return (
      <ItemsList static={true}>
        { this.getStaticFields() }
        { this.getFields() }
        <BackTop/>
      </ItemsList>
    );
  }
}

export default Metadata;
