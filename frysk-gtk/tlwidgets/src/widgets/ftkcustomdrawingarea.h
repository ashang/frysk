/* Copyright (C) 2006 Red Hat, Inc.
   This file is part of the Red Hat Customdrawingarea GTK+ widget.

   Red Hat Customdrawingarea is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by the
   Free Software Foundation; version 2 of the License.

   Red Hat Customdrawingarea is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with Red Hat Customdrawingarea; if not, write to the Free Software Foundation,
   Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301 USA. */

#ifndef __FTK_CUSTOMDRAWINGAREA_H__
#define __FTK_CUSTOMDRAWINGAREA_H__

#include <glib.h>
#include <glib-object.h>

#include "ftkcustomatkobject.h"

#define USE_SLIDER_INTERVAL

G_BEGIN_DECLS

typedef struct _FtkCustomDrawingArea
  {
    GtkDrawingArea	parent;
    FtkCustomAtkObject*           accessible;
  }
FtkCustomDrawingArea;

typedef struct _FtkCustomDrawingAreaClass
  {
    GtkDrawingAreaClass parent_class;
  }
FtkCustomDrawingAreaClass;

/*************** public api *****************/

AtkObject* ftk_custom_drawing_area_get_accessible (GtkWidget* da);
void       ftk_custom_drawing_area_set_accessible (FtkCustomDrawingArea* da, FtkCustomAtkObject* acessible);
void       ftk_custom_drawing_area_init (FtkCustomDrawingArea* da);
GtkType    ftk_custom_drawing_area_get_type(void);
GtkWidget* ftk_custom_drawing_area_new();
           
/*
GType
ftk_custom_drawing_area_get_type		(void);


GtkWidget *
ftk_custom_drawing_area_new		();

*/

G_END_DECLS

#endif /* __FTK_CUSTOMDRAWINGAREA_H__ */
