import { registerDecorator, ValidationOptions, ValidationArguments } from 'class-validator';

export function IsStrongPassword(validationOptions?: ValidationOptions) {
  return function (object: Object, propertyName: string) {
    registerDecorator({
      name: 'isStrongPassword',
      target: object.constructor,
      propertyName: propertyName,
      options: {
        message:
          'Password must be at least 8 characters with 1 uppercase, 1 lowercase, 1 number, and 1 special character',
        ...validationOptions,
      },
      validator: {
        validate(value: any) {
          if (typeof value !== 'string') return false;
          if (value.length < 8) return false;
          if (!/[A-Z]/.test(value)) return false;
          if (!/[a-z]/.test(value)) return false;
          if (!/[0-9]/.test(value)) return false;
          if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value)) return false;
          return true;
        },
      },
    });
  };
}
