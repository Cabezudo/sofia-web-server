/*
 * Created on: 30/10/2018
 * Author:     Esteban Cabezudo
 */

/* global Core */

const editableField = ({ element = null, id = null, validationURI = null, updateURI = null, field = null, defaultValue = null, onValid = null, onNotValid = null, onUpdate = null } = {}) => {
  let inputElement, saveButton, lastValue, validationTimer, saveTimer, requestId = 0;

  const validateOptions = () => {
    if (element === null && id === null) {
      throw Error('You must define a property id or a property element.');
    }
    if (validationURI === null) {
      throw Error('You must set a web services validationURI.');
    }
    if (updateURI === null) {
      throw Error('You must set a web services storeURI.');
    }
    if (field === null) {
      throw Error('You must add a record field to associate.');
    }
  };
  const createGUI = () => {
    if (element === null) {
      element = Core.validateById(id);
    }
    element.classList.add('editableField');
    inputElement = document.createElement('input');
    inputElement.setAttribute('type', 'text');
    inputElement.value = defaultValue;
    lastValue = defaultValue;
    element.appendChild(inputElement);
    element.inputElement = inputElement;
    inputElement.data = {validationURI, field};

    saveButton = document.createElement('div');
    element.appendChild(saveButton);
  };
  const sendValidationRequest = event => {
    const inputElement = event.srcElement;
    const data = {
      field: inputElement.data.field,
      value: inputElement.value
    };
    const uri = validationURI.replace('{value}', inputElement.value);
    const response = Core.sendGet(uri, inputElement, data);
    requestId = response.requestId;
  };
  const sendUpdateRequest = () => {
    saveButton.innerHTML = 'Saving...';
    saveButton.className = 'saving';
    const data = {
      field: inputElement.data.field,
      value: inputElement.value
    }
    const response = Core.sendPut(updateURI, inputElement, data);
  };
  const assignTriggers = () => {
    inputElement.addEventListener('response', event => {
      const data = event.detail;
      console.log(data);
      if (data.status === 'OK') {
        if (data.type === 'UPDATE') {
          saveButton.innerHTML = 'Saved';
          saveButton.className = 'saved';
          saveButton.style.opacity = 0;
          saveButton.style.pointerEvents = 'none';
          if (Core.isFunction(onUpdate)) {
            onUpdate();
          }
        }
      }
      if (requestId === data.requestId) {
        element.classList.remove('error');
        Core.showMessage(data);
        if (data.status === 'ERROR') {
          element.classList.add('error');
        }
        if (data.status === 'OK') {
          if (data.type === 'VALIDATION') {
            saveButton.innerHTML = 'Save';
            saveButton.className = '';
            saveButton.style.opacity = 1;
            saveButton.style.pointerEvents = 'auto';
            saveTimer = setTimeout(event => {
              sendUpdateRequest(event);
            }, 8000);
            if (Core.isFunction(onValid)) {
              onValid();
            }
          }
        } else {
          if (Core.isFunction(onNotValid)) {
            onNotValid();
          }
        }
      }
    });
    const update = event => {
      if (Core.isModifierKey(event) || Core.isNavigationKey(event)) {
        return;
      }
      if (lastValue !== inputElement.value) {
        if (saveTimer) {
          clearTimeout(saveTimer);
        }
        lastValue = inputElement.value;
        if (validationTimer) {
          clearTimeout(validationTimer);
        }
        validationTimer = setTimeout(() => {
          sendValidationRequest(event);
        }, 400);
      }
    };
    sendData = event => {
      const source = event.srcElement;
      const data = source.data;
    };
    element.addEventListener("keyup", event => {
      update(event);
    });
  };
  validateOptions();
  createGUI();
  assignTriggers();
}
;