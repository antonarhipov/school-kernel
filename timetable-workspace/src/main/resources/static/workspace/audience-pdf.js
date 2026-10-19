import { rgb } from './vendor/pdf/pdf-lib.js';
import { audienceCell, shortGroupName } from './audience-model.js';
import { M } from './messages.js';
import { PDF_SLOT_WIDTH, lessonColor, slotLabels } from './pdf-style.js';

// All printable lesson information belongs in the timetable itself.
export function drawAudiencePdf({ model, selected, pdf, sheets, schoolName, timetableName, draft,
  drawText, box, linesFor, regular, bold, width, height, margin, bottom }) {
  const ink=rgb(.12,.12,.12), muted=rgb(.4,.4,.4), pale=rgb(.94,.94,.94), white=rgb(1,1,1), green=rgb(.12,.32,.24);
  const usable=width-2*margin, labelWidth=PDF_SLOT_WIDTH, cellWidth=(usable-labelWidth)/selected.days.length;
  if(model.result.schemaVersion!==2 || !['FEASIBLE','REPAIRED'].includes(model.result.status))
    throw new Error('The selected pair has no supported reviewed audience result. Review it before export.');
  const allIds=new Set();
  for(const a of model.assignments) {
    if(allIds.has(a.lessonId) || !a.lesson || !a.audience || !a.subject || !a.teacher || !a.room || !a.period || !a.cohort)
      throw new Error(`Cannot resolve complete printable details for session ${a.lessonId}. Export refused.`);
    allIds.add(a.lessonId);
  }
  if(allIds.size!==model.maps.lessons.size)throw new Error('The printable pair has missing sessions. Review the complete timetable before export.');
  const lessonLines=(a, available, classView)=>[
    ...linesFor(a.subject.displayName,9,bold,available),
    ...(a.audience.id!==`class:${a.cohortId}`?linesFor(shortGroupName(a.audience),8,regular,available):[]),
    ...linesFor(classView?a.teacher.displayName:`Class ${a.cohort.displayName}`,8,regular,available),
    ...linesFor(a.room.displayName,8,regular,available,muted)
  ];
  for(const entity of selected.entities) {
    const items=selected.type==='school'?selected.items.filter(a=>a.cohortId===entity.id):selected.items;
    const classView=selected.type!=='teacher';
    const title=`${classView?'Class':'Teacher'} ${entity.displayName??entity.id}`;
    let page,y,part=0;
    const newGrid=()=>{
      page=pdf.addPage([width,height]);sheets.push(page);part++;
      box(page,0,0,width,6,green,green);
      drawText(page,'S C H O O L   K E R N E L',margin,22,8,bold,green);
      drawText(page,draft?'DRAFT - Unpublished changes':'TIMETABLE',width-margin-210,22,9,bold,muted);
      y=43;
      for(const [value,size,font] of [[schoolName,17,bold],[`${timetableName} / ${title}${part>1?' / continued':''}`,11,bold]]) {
        for(const line of linesFor(value,size,font,usable)) {drawText(page,line.text,margin,y,size,font);y+=size+3;}
      }
      y+=15;
      if(y+25>bottom-65)throw new Error('The page headings are too long for the PDF page. Shorten the titles and try again.');
      box(page,margin,y,usable,25,green,green);
      drawText(page,'Slot',margin+5,y+7,8,bold,white);
      selected.days.forEach((d,i)=>drawText(page,M.days[d]??d,margin+labelWidth+i*cellWidth+5,y+7,9,bold,white));y+=25;
    };
    newGrid();
    const occupied=items.map(a=>a.period.order);
    const orders=occupied.length?selected.orders.filter(o=>o>=Math.min(...occupied)&&o<=Math.max(...occupied)):selected.orders;
    for(const order of orders) {
      const labels=slotLabels(selected,order);
      const cells=selected.days.map((day,di)=>{
        const period=selected.periods[di].find(p=>p.order===order);
        const visible=items.filter(a=>a.periodId===period?.id);
        const cell=classView&&period?audienceCell(model,entity.id,period.id,visible):null;
        const lanes=cell?.context.lanes?.length??0;
        // Keep readable side-by-side groups; crowded or crossing groups use stacked lessons.
        if(visible.length && cell?.spans?.length && cell.spans.every(s=>cellWidth*s.length/lanes>=24))
          return cell.spans.map(s=>({x:cellWidth*s.start/lanes,width:cellWidth*s.length/lanes,
            color:s.item?lessonColor(model,s.item):pale,
            lines:s.item?lessonLines(s.item,cellWidth*s.length/lanes-8,classView):linesFor(shortGroupName(s.audience),8,regular,cellWidth*s.length/lanes-8)}));
        if(!visible.length)return [{x:0,width:cellWidth,color:white,lines:period?.nonTeaching?linesFor(`${period.nonTeaching}: ${period.displayName}`,8,regular,cellWidth-8):[]}];
        return [{x:0,width:cellWidth,color:lessonColor(model,visible[0]),lines:visible.flatMap((a,i)=>[
          ...(i?[{text:'',size:8,font:regular,color:muted}]:[]),...lessonLines(a,cellWidth-8,classView)
        ])}];
      });
      const totalLines=Math.max(4,labels.length,...cells.flatMap(c=>c.map(l=>l.lines.length)));
      let offset=0;
      while(offset<totalLines) {
        const desired=(totalLines-offset)*10+10;
        if(y+Math.min(desired,50)>bottom || (desired<=bottom-160 && y+desired>bottom))newGrid();
        const count=Math.min(totalLines-offset,Math.floor((bottom-y-10)/10));
        if(count<1)throw new Error('The page headings leave insufficient space for lessons. Shorten the titles and try again.');
        const rowHeight=count*10+10;
        box(page,margin,y,labelWidth,rowHeight,white,ink);
        labels.slice(offset<labels.length?offset:0,(offset<labels.length?offset:0)+count).forEach((label,i)=>drawText(page,label,margin+5,y+7+i*10,8,regular,muted));
        cells.forEach((lanes,di)=>lanes.forEach(lane=>{
          const x=margin+labelWidth+di*cellWidth+lane.x;
          box(page,x,y,lane.width,rowHeight,lane.color,ink);
          lane.lines.slice(offset,offset+count).forEach((line,i)=>drawText(page,line.text,x+4,y+5+i*10,line.size,line.font,line.color));
        }));
        y+=rowHeight;offset+=count;
        if(offset<totalLines)newGrid();
      }
    }
  }
}
