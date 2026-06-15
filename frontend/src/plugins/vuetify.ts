import { createVuetify } from 'vuetify'
import 'vuetify/styles'

const theme = {
  dark: false,
  themes: {
    light: {
      colors: {
        primary: '#1E3A5F',
        secondary: '#4F46E5',
        accent: '#0EA5E9',
        error: '#DC2626',
        info: '#0EA5E9',
        success: '#16A34A',
        warning: '#F59E0B',
        surface: '#FFFFFF',
        background: '#F1F5F9',
        'primary-darken-1': '#15294A',
        'primary-lighten-1': '#2D4A70',
        'grey-lighten-5': '#F8FAFC',
        'grey-darken-3': '#334155',
      },
    },
  },
}

export default createVuetify({
  theme,
  defaults: {
    VCard: {
      elevation: 0,
      class: 'rounded-xl border',
    },
    VTextField: {
      variant: 'outlined',
      density: 'comfortable',
      hideDetails: 'auto',
      color: 'primary',
    },
    VSelect: {
      variant: 'outlined',
      density: 'comfortable',
      hideDetails: 'auto',
      color: 'primary',
    },
    VTextarea: {
      variant: 'outlined',
      density: 'comfortable',
      hideDetails: 'auto',
    },
    VBtn: {
      density: 'comfortable',
      class: 'text-none font-weight-medium',
    },
    VDialog: {
      width: 480,
    },
    VNavigationDrawer: {
      floating: false,
    },
    VAutocomplete: {
      variant: 'outlined',
      density: 'comfortable',
      hideDetails: 'auto',
      color: 'primary',
    },
    VChip: {
      class: 'font-weight-medium',
    },
    VTable: {
      density: 'comfortable',
    },
  },
})
