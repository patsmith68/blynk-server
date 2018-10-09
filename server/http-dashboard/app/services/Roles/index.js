export const SUPER_ADMIN_ROLE_ID = 0;
export const SUPER_ADMIN_ROLE_TITLE = 'Super Admin';

export const filterSuperAdmin = (roles = []) => {
  return roles.filter((role) => role && role.id && role.id !== SUPER_ADMIN_ROLE_ID);
};

export const formatRolesToKeyValueList = (roles) => {
  return roles.map((role) => {
    return {
      key: `${role.id}`,
      value: `${role.name}`
    };
  });
};

export const ORG_INVITE_ROLE_ID = 1;

const Roles = {
  'SUPER_ADMIN': {
    value: 'SUPER_ADMIN',
    title: 'Super Admin'
  },
  'ADMIN': {
    value: 'ADMIN',
    title: 'Admin'
  },
  'STAFF': {
    value: 'STAFF',
    title: 'Staff'
  },
  'USER': {
    value: 'USER',
    title: 'User'
  }
};

const AvailableRoles = {
  'ADMIN': {
    key: 'ADMIN',
    value: 'Admin'
  },
  'STAFF': {
    key: 'STAFF',
    value: 'Staff'
  },
  'USER': {
    key: 'USER',
    value: 'User'
  }
};

const InviteAvailableRoles = [
  AvailableRoles.ADMIN,
  AvailableRoles.STAFF,
  AvailableRoles.USER
];

const UsersAvailableRoles = [
  Roles.ADMIN,
  Roles.STAFF,
  Roles.USER
];

const MetadataRoles = [
  {
    key: Roles.ADMIN.value,
    value: Roles.ADMIN.title
  },
  {
    key: Roles.STAFF.value,
    value: Roles.STAFF.title
  },
  {
    key: Roles.USER.value,
    value: Roles.USER.title
  }
];

const MetadataRolesDefault = [];

export {
  MetadataRoles,
  MetadataRolesDefault,
  Roles,
  InviteAvailableRoles,
  UsersAvailableRoles
};

export const isUserAbleToEdit = (userRole, fieldRole) => {

  if(userRole === SUPER_ADMIN_ROLE_ID)
    return true;

  return (Array.isArray(fieldRole) ? fieldRole : [fieldRole]).indexOf(Number(userRole)) !== -1;
};
