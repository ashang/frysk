/* Copyright (C) 2006 Red Hat, Inc.
   This file is part of the Red Hat CustomDrawingArea GTK+ widget.

   Red Hat CustomDrawingArea is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat CustomDrawingArea is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat CustomDrawingArea; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA. */

#define _GNU_SOURCE
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <math.h>
#include <sys/types.h>
#include <sys/time.h>
#include <time.h>
#include <gtk/gtk.h>
#include <gtk/gtkwidget.h>
#include <gtk/gtkbindings.h>


#include "ftkcustomdrawingarea.h"


//G_DEFINE_TYPE (FtkCustomDrawingArea, ftk_custom_drawing_area, ftk_custom_drawing_area_get_type());


void ftk_custom_drawing_area_class_init (FtkCustomDrawingAreaClass *klass)
{
  GtkWidgetClass * widget_class;

  widget_class = (GtkWidgetClass *) klass;
  widget_class->get_accessible = ftk_custom_drawing_area_get_accessible;
}

GType
ftk_custom_drawing_area_get_type ()
{
  static GType custom_drawing_area_type = 0;

  if (!custom_drawing_area_type)
    {
      static const GTypeInfo custom_drawing_area_info =
        {
          sizeof (FtkCustomDrawingAreaClass),		/* class_size		*/
          NULL, 					/* base_init		*/
          NULL,					/* base_finalize	*/
          (GClassInitFunc) ftk_custom_drawing_area_class_init,	/* class_init	*/
          NULL, 					/* class_finalize	*/
          NULL, 					/* class_data		*/
          sizeof (FtkCustomDrawingArea),			/* instance size	*/
          0,					/* n_preallocs		*/
          (GInstanceInitFunc) ftk_custom_drawing_area_init,	/* instance_init	*/
        };

      custom_drawing_area_type = g_type_register_static (GTK_TYPE_DRAWING_AREA,
                         "Gtk_CustomDrawingArea",
                         &custom_drawing_area_info, 0);
    }

  return custom_drawing_area_type;
}


void ftk_custom_drawing_area_init (FtkCustomDrawingArea* da)
{

}


GtkWidget* ftk_custom_drawing_area_new ()
{
  FtkCustomDrawingArea * da = g_object_new (ftk_custom_drawing_area_get_type(),NULL);
  return GTK_WIDGET (da);
}

AtkObject * ftk_custom_drawing_area_get_accessible (GtkWidget *widget)
{
  return (AtkObject*)(((FtkCustomDrawingArea*)widget)->accessible);
}

void ftk_custom_drawing_area_set_accessible (FtkCustomDrawingArea* da, FtkCustomAtkObject* accessible)
{
  da->accessible = accessible;
}
